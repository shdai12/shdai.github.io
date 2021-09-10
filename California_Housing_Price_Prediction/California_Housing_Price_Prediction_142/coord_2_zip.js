var gps = require('gps2zip');
var latitude = 34.09768;
var longitude = -118.34602;
// console.log(gps.gps2zip(latitude, longitude));
// returns 78701

var latlong = process.argv.slice(2);

lat = latlong[0]
long = latlong[1]


var gps_stats = gps.gps2zip(lat, long)

console.log(gps_stats["zip_code"]);