use( "RawRepoIndexCreator" );
use( "XmlUtil" );
use( "Log" );

/**
 * Entry point for the Javascript solr indexer code
 *
 * @type {function}
 * @syntax index( content, suppDataRaw )
 * @param {String} content The collection of records in a MarcXChange collection encoded as a String
 * @param {Object} suppDataRaw JSON DTO encoded as a String which has supplementary data for the Javascript
 *        {String} mimetype The mimetype of the main record
 *        {Boolean} isHeadOrSectionHasChildren True if the main record is a head or section record and the record has children. Otherwise false
 *        {Integer} correctedAgencyId The "real" agency id of the main record
 * @return {String} The JSON document as a String to store in solr
 * @name index
 * @function
 */
var index = function (content, suppDataRaw) {
    Log.info( "Hejsa" );
    var suppData = JSON.parse(suppDataRaw);
    var xmlContent = XmlUtil.fromString( content );

    Log.info( "Entering solr-indexer-basis javascript entrypoint with content = " + content
        + " and SupplementaryData = " + uneval(suppData));

    //indexObject below has index names as keys. Values are strings in an array
    var indexObject = RawRepoIndexCreator.createBasisIndexData( xmlContent, suppData );

    Log.info( "Leaving solr-indexer-basis javascript entrypoint" );

    return JSON.stringify( indexObject );
};

var index_dit_wrapper = function (content, args) {
    return index(content, JSON.stringify(args))
};
