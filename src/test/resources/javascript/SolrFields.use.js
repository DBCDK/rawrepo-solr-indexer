/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

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