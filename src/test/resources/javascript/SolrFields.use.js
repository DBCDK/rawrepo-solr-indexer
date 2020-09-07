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
    var that = {};
    that.indexObject = {};

    that.addField = function(key, value) {
        var valueList = this.indexObject[key]
        if (valueList === undefined) {
           this.indexObject[key] = []
        }
        this.indexObject[key].push(value);
    };

    return that;
}( );

var solrField = function (key, value) {
    SolrFields.addField(key, value)
}