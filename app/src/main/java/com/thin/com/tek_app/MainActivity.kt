package com.thin.com.tek_app

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.app.ActivityCompat
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.location.*
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task

import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mDrawerLayout: DrawerLayout
    private var REQUEST_LOCATION_CODE = 101
    private var REQUEST_CHECK_SETTINGS = 0x1

    val fm = supportFragmentManager
    private lateinit var fragment: Fragment
    lateinit var latLng: LatLng
    lateinit var mLocationRequest: LocationRequest
    lateinit var mLocationCallback: LocationCallback
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var mSettingsClient: SettingsClient
    lateinit var mLocationSettingsRequest: LocationSettingsRequest
    lateinit var location: Location

    lateinit var mGeoDataClient: GeoDataClient
    var isAutoCompleteLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(savedInstanceState == null){
            fragment = DashboardFragment::class.java.newInstance() as Fragment
            addFragment(fragment,R.id.flContent,"dashboard_fm")
        }

        var toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"))
        setSupportActionBar(toolbar)

        var actionBar: ActionBar? = supportActionBar
        actionBar?.apply{
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        supportActionBar?.setTitle("Round Trip - SG")

        mDrawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }

        checkGPSEnabled()

        mLocationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                location = locationResult!!.lastLocation
                latLng = LatLng(location.latitude,location.longitude)
                val msg = "Updated Location: " +
                        location.latitude.toString() + "," +
                        location.longitude.toString()

                Log.d("Updated Location",msg)
                setDashboardTitle()
            }
        }

        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval((10*1000).toLong()) // 10 secs, in milliseconds
            .setFastestInterval((6*1000).toLong()) //1 sec in milliseconds

        mSettingsClient = LocationServices.getSettingsClient(this)

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()

    }

    private fun checkGPSEnabled():Boolean{
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var isLocationEnable =  locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)

        if(!isLocationEnable){
            showAlert()
        }

        return isLocationEnable
    }

    private fun setDashboardTitle(){

        var chkLat = latLng.latitude.toInt()
        var current_fm = fm.findFragmentByTag("dashboard_fm")

        if(current_fm != null && current_fm.isVisible){
            if(chkLat == 0){
                supportActionBar?.setTitle("Round Trip - SG")
            }else{
                var geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                var addresses:List <Address>  = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )

                var address = addresses[0].getAddressLine(0)
                supportActionBar?.setTitle(address)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_LOCATION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    initLocation()
                }

            }else{
                showAlert()
            }

            return
        }
    }

    override fun onResume() {
        super.onResume()
        if(checkPermission()){
            initLocation()
        }else{
            checkLocationPermission()
        }

    }

    private fun initLocation() {
        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            getLastLocation()
            try {
                mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(this, object : OnSuccessListener<LocationSettingsResponse> {
                        override fun onSuccess(p0: LocationSettingsResponse?) {
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                        }

                    })

            } catch (unlikely: SecurityException) {
                Log.e("Location", "Lost location permission. Could not request updates. " + unlikely)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLastLocation(){
        try{
           mFusedLocationClient.lastLocation.addOnCompleteListener{task: Task<Location> ->
               if(task.isSuccessful && task.result != null){
                   location = task.getResult() as Location
                   latLng = LatLng(location.latitude, location.longitude)

                   val msg = "Updated Location: " +
                           location.latitude.toString() + "," +
                           location.longitude.toString()
                   Log.d("Last Location",msg)
                   setDashboardTitle()

               }else{

                   Log.w("Location","Failed to get location")
                   latLng = LatLng(0.0,0.0)
                   setDashboardTitle()
               }
           }

        }catch (unlikely: SecurityException){
            Log.e("Location","Lost Location Permission."+unlikely)
        }
    }

    private fun checkPermission(): Boolean{
        var permissionState = ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun showAlert(){
        val dialog = AlertDialog.Builder(this )
        dialog.setTitle("Enable Location")
            .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
            .setPositiveButton("Location Settings") { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            .setNegativeButton("Cancel") { paramDialogInterface, paramInt -> }
        dialog.show()
    }

    private fun checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
                    })
                    .create()
                    .show()

            } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
        }
    }

    private fun selectDrawerItem(menuItem:MenuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        var fragmentClass:Class<*>
        var fragmentTagName: String =""
        when (menuItem.itemId) {
            R.id.nav_dashboard -> {
                fragmentClass = DashboardFragment::class.java
                fragmentTagName = "dashboard_fm"
            }
            R.id.nav_topic -> {
                fragmentClass = TopicFragment::class.java
                fragmentTagName = "topic_fm"
            }
            R.id.nav_bus -> {
                fragmentClass = BusFragment::class.java
                fragmentTagName = "bus_fm"
            }
            R.id.nav_privacy -> {
                fragmentClass = PrivacyFragment::class.java
                fragmentTagName = "privacy_fm"
            }
            R.id.nav_about -> {
                fragmentClass = AboutFragment::class.java
                fragmentTagName = "about_fm"
            }
            R.id.nav_place -> {
                fragmentClass = PlaceFragment::class.java
                fragmentTagName = "place_fm"
            }
            R.id.nav_announcement -> {
                fragmentClass = AnnounceFragment::class.java
                fragmentTagName = "annocuement_fm"
            }
            R.id.nav_setting -> {
                fragmentClass = SettingFragment::class.java
                fragmentTagName = "setting_fm"
            }
            R.id.nav_sign_in -> {
                fragmentClass = SignInFragment::class.java
                fragmentTagName = "sign_in_fm"
            }
            else -> {
                fragmentClass = DashboardFragment::class.java
                fragmentTagName = "dashboard_fm"
            }
        }

        try
        {
            fragment = fragmentClass.newInstance() as Fragment
        }
        catch (e:Exception) {
            e.printStackTrace()
        }

        replaceFragment(fragment, R.id.flContent,fragmentTagName)

        var menuTitle = menuItem.title.toString()

        if (menuItem.itemId == R.id.nav_dashboard){
            getLastLocation()
        }

        supportActionBar?.setTitle(menuTitle)
        menuItem.isChecked = true
        mDrawerLayout.closeDrawer(GravityCompat.START)

    }

    override fun onOptionsItemSelected(item: android.view.MenuItem?): kotlin.Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun logoClick(view: View){
        mDrawerLayout.closeDrawers()
    }

    inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
    }

    fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int,tag: String){
        supportFragmentManager.inTransaction { add(frameId, fragment, tag) }
    }

    fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int, tag: String) {
        supportFragmentManager.inTransaction{replace(frameId, fragment,tag)}
    }

}


