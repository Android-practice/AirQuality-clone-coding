package com.androidpractice.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat

/*
Android Location Info

1. GPS based
2. NETWORK based


if both available -> return more accurate location info
if one available -> return available location info

else -> return null

 */

class LocationProvider(val context:Context) {
    private var location : Location? = null //latitude, longitude based location
    private var locationManager : LocationManager? = null


    init{
        getLocation()
    }


    private fun getLocation() : Location? {

        try{
            //mainActivity는 context를 가지고 있어서 getSystemService함수를 그냥 호출 가능
            // but locationProvider class는 없기 떄문에 인자로 받아서 context를 이용해 사용해야함
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var gpsLocation : Location? = null
            var networkLocation : Location? = null

            //GPS or Network is available

            val isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) //!! fail to catch
            val isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)


            //both not available
            if(!isGPSEnabled && !isNetworkEnabled){
                return null
            }else{

                //add permission check
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return null
                }

                if(isNetworkEnabled){
                    networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                if(isGPSEnabled){
                    gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }


                //if both available -> use more accurate one
                if(gpsLocation!=null && networkLocation !=null){

                    if(gpsLocation.accuracy > networkLocation.accuracy){
                        location = gpsLocation
                    }else{
                        location = networkLocation
                    }

                }else { // one is available
                    if(gpsLocation != null){
                        location = gpsLocation
                    }
                    if(networkLocation!=null){
                        location = networkLocation
                    }
                }


            }

        }catch(e:Exception){
            e.printStackTrace()
        }

        return location

    }


    fun getLocationLatitude(): Double?{
        return location?.latitude
    }


    fun getLocationLongitude(): Double?{
        return location?.longitude
    }

}