/**
 * Only used for DIT testsuites
 *
 * exports mocked SolrFields via solrField function
 */
EXPORTED_SYMBOLS = [ 'SolrFields', 'solrField' ];

var SolrFields = function() {
    var indexObject = {};

    function addField(key, value) {
        var valueList = indexObject[key]
        if (valueList === undefined) {
           indexObject[key] = []
        }
        indexObject[key].push(value);
    }

    function getIndexObject() {
        return indexObject;
    }

    return {
        addField: addField,
        getIndexObject: getIndexObject
    }
}();

var solrField = function (key, value) {
    SolrFields.addField(key, value)
}
