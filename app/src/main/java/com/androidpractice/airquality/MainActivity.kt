package com.androidpractice.airquality

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.androidpractice.airquality.databinding.ActivityMainBinding
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var locationProvider: LocationProvider

    private val PERMISSION_REQUEST_CODE = 100//request를 식별하는 코드 (id값)

    val REQURIED_PERMISSIONS = arrayOf( //요청하려는 권한 리스트
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    lateinit var getGPSPermissinonLauncher: ActivityResultLauncher<Intent> //서로 다른 Activity로 넘어가고 다시 돌아오면서 데이터를 넘겨줄 때 사용

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
    }


    private fun updateUI(){
        locationProvider = LocationProvider(this@MainActivity)

        var latitude : Double? = locationProvider.getLocationLatitude()
        var longtitude : Double? = locationProvider.getLocationLongitude()

        if(latitude != null && longtitude != null){
            //1. get current location(address) with latitude/longitude info -> ui update
            val address = getCurrentAddress(latitude,longtitude)

            //ui update
            address?.let{//run only when != null
                binding.tvLocationTitle.text = "${it.thoroughfare}"
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"

            }

            //2. get yellow dust info -> ui update

        }else{
            Toast.makeText(this, "위도, 경도 정보를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
        }

    }


    private fun getCurrentAddress(latitude: Double, longitude: Double) : Address?{
        val geoCoder = Geocoder(this, Locale.getDefault()) // get location based on default setting(KOREA..)
        val addresses : List<Address>?

        addresses = try{
            geoCoder.getFromLocation(latitude,longitude, 7)//max number of address
        }catch(ioException:IOException){
            Toast.makeText(this, "지오코더 서비스를 사용 불가능 합니다.", Toast.LENGTH_LONG).show()
            return null
        }catch(illegalArgumentException: java.lang.IllegalArgumentException){
            Toast.makeText(this, "잘못된 위도, 경도 값입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        if(addresses == null || addresses.size == 0){
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }

        return addresses[0]

    }

    private fun checkAllPermissions(){

         if(!isLocationServicesAvailable()){ //GPS서비스 꺼짐
            showDialogForLocationServiceSetting() //GPS서비스 요청
         }else{//GPS서비스 켜짐
             isRuntimePermissionGranted()//런타임 권한 확인
         }
    }

    private fun isLocationServicesAvailable() : Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager//캐스팅

        //위성신호(GPS), 와이파이네트워크를 이용한 위치파악(NETWORK)
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    private fun isRuntimePermissionGranted(){
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)

        //위의 권한 2개중 1개라도 안되어 있는 경우
        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED){
            //권한을 요청함(권한 요청 팝업)
            ActivityCompat.requestPermissions(this@MainActivity, REQURIED_PERMISSIONS, PERMISSION_REQUEST_CODE) //(,PERMISSION요청 리스트, permission요청 코드(요청 권한의 id))
        }
    }

    //권한요청 결과 받는 법
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //요청한 request가 맞는지 code/size로 검증(요청한 권한이 여러개 일 수있음)
        if(requestCode === PERMISSION_REQUEST_CODE && grantResults.size == REQURIED_PERMISSIONS.size){
            var checkResult = true

            //필요한 "모든"권한이 허용되었는지 체크
            for (result in grantResults){
                if (result != PackageManager.PERMISSION_GRANTED){ //권한 허용x -> 필요한 것중 1개라도 안되어 있으면 앱을 종료해야하므로 break
                    checkResult = false
                    break
                }
            }

            if(checkResult){//모든 권한이 허용됨
                //모든 permission통과 -> 위치값 가져올 수 있음
                updateUI()



            }else{//권한 허용x -> 토스트 메세지와 함께 앱 종료
                Toast.makeText(this@MainActivity, "권한이 거부되었습니다 앱을 다시 실행하여 권한을 허용해주세요", Toast.LENGTH_LONG).show()
                finish() //앱 종료
            }
        }
    }

    //launcher
    private fun showDialogForLocationServiceSetting(){
        getGPSPermissinonLauncher = registerForActivityResult( //activityResultLauncher 객체 생성에 사용
            ActivityResultContracts.StartActivityForResult()//parameter1 : 어떤activity contract인지 / parameter2 : callback함수(result를 받아서 함수를 실행시킴)
        ){
            result ->
            if(result.resultCode == Activity.RESULT_OK){ //result가 제대로 온 경우
                if(isLocationServicesAvailable()){//GPS추적 사용중인지
                    isRuntimePermissionGranted()//앱에서 gps정보 사용 가능한지
                }else{
                    Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.",Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }


        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져있습니다. 위치 서비스를 활성화 해야 앱을 사용가능합니다.")
        builder.setCancelable(true) //dialog 박스 이외의 곳을 터치하면 닫히도록
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialogInterface, i ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)//안드로이드 설정으로 이동
            getGPSPermissinonLauncher.launch(callGPSSettingIntent)// launch(어떤 activity contract인지) -> 제대로 돌아오면 GPS사용중, 사용가능여부 체크 ...(call back함수 내용)
        })

        builder.setNegativeButton("취소", DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.cancel()
            Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.",Toast.LENGTH_LONG).show()
            finish()
        })

        builder.create().show() //생성

    }


}