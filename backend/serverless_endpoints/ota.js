const path = require( 'path' )
const { Storage } = require( '@google-cloud/storage' )
const { BigQuery } = require( '@google-cloud/bigquery' )

const BQ_DATASET = 'ota'
const BQ_TABLE = 'firmware'
const TABLE_SCHEMA = 'id:string, eventType: string, bucket:string, version:string, device:string, fullname:string, createdAt:timestamp'
const projectId = 'smart-latch'

const bqClient = new BigQuery( { projectId } )
const storage = new Storage()
const bucket = storage.bucket( `${projectId}-esp-firmware` )


async function insertIntoBigquery( data ) {
  const [ datasets ] = await bqClient.getDatasets()
  let dataset = datasets.find( dataset => dataset.id === BQ_DATASET )  
  console.log( 'Datasets', datasets, dataset )
  // let dataset = bqClient.dataset( BQ_DATASET )
  if ( !dataset ) {
    [ dataset ] = await bqClient.createDataset( BQ_DATASET )  
  }

  const [ tables ] = await dataset.getTables()
  let table = tables.find( table => table.id === BQ_TABLE )
  console.log( 'Tables', tables, table )
  // let table = dataset.table( BQ_TABLE )    
  if ( !table ) {
    [ table ] = await dataset.createTable( BQ_TABLE, { schema : TABLE_SCHEMA } )
  }
  
  try {
    const res = await table.insert( data, { ignoreUnknownValues : true } );
    console.log( 'Insert', res )
  } catch ( e ) {
    console.log( 'Insert Failed', e, JSON.stringify( e ) )        
  }
}

async function getPublicUrl( filename ) {
  const file = bucket.file( filename )    
  await file.makePublic()  
  const url = `http://${bucket.name}.storage.googleapis.com/${filename}`
  return url
}

/**
 * Generic background Cloud Function to be triggered by Cloud Storage.
 *
 * @param {object} data The event payload.
 * @param {object} context The event metadata.
 */
exports.insertFirmwaresOnBigquery = ( data, context ) => {
  const file = data;
  
  const filename = path.basename( file.name )
  const ext = path.extname( filename )
  if ( ext !== '.bin' ) {
    console.log( 'Not a firmware file.' )
    return 'ok'
  }
  
  const filenameWithoutExt = path.basename( file.name, ext )
  const dir = path.dirname( file.name )
  const version = filenameWithoutExt.split('_')[1]
  const device = filenameWithoutExt.split('_')[2]
  const row = { 
    id : context.eventId,
    bucket : file.bucket,
    version,
    device,
    fullname : file.name,
    createdAt : file.timeCreated,
    eventType : context.eventType,
  }

  console.log( 'Row to insert', row )
  return insertIntoBigquery( row )
};


/**
 * Responds to any HTTP request.
 * @param {!express:Request} req HTTP request context.
 * @param {!express:Response} res HTTP response context.
 */
exports.getDownloadUrl = async ( req, res ) => {
  try {
    const { version, device } = req.query
    console.log( 'Fetch version and device ', version, device )    
    
    const queryParams = {
      query : `
        SELECT 
          bucket,
          fullname,
          version,
          createdAt
        FROM \`smart-latch.ota.firmware\`
        where device = @device
        order by createdAt desc
        limit 1      
      `,
      params : {
        device
      }
    }

    const [ rows ] = await bqClient.dataset( BQ_DATASET ).table( BQ_TABLE ).query( queryParams )
    if ( rows.length > 0 ) {
      const firmware = rows[0]
      
      // latest > current
      console.log('latest version ', version)  
      console.log('current version', firmware.version)  
      const needsUpdate = version < firmware.version     
      if ( needsUpdate ) {
        const url = await getPublicUrl( firmware.fullname )
        console.log( 'Sending url', url )
        res.status( 200 ).send( url );
      } else {
        res.status( 204 ).send( 'Up to date' );  
      }

    } else {
      res.status( 204 ).send( 'No new version' );
    }

  } catch ( err ) {
    console.log('Error', err)  
    res.status( 500 ).send( err.message );
  }
}