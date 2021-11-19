EXPORTED_SYMBOLS = [ 'RawRepoIndexCreator' ];

var RawRepoIndexCreator = function() {
    Log.info( "Entering RawRepoIndexCreator module" );

    function createBasisIndexData( xmlRecords, suppData ) {
        Log.trace( "Entering RawRepoIndexCreator.createBasisIndexData" );

        Log.trace( "Leaving RawRepoIndexCreator.createBasisIndexData" );

        return []
    }

    return {
        createBasisIndexData: createBasisIndexData
    }
}
