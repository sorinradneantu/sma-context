package ro.upt.sma.context

import android.Manifest.permission
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.text.MessageFormat
import ro.upt.sma.context.activity.ActivityRecognitionHandler
import ro.upt.sma.context.activity.ActivityRecognitionService
import ro.upt.sma.context.location.LocationHandler

class ContextActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fMap: SupportMapFragment
    private lateinit var tvLocation: TextView
    private lateinit var tvActivity: TextView

    private var googleMap: GoogleMap? = null

    private lateinit var locationHandler: LocationHandler
    private lateinit var activityRecognitionHandler: ActivityRecognitionHandler
    private var locationCallback: LocationCallback? = null
    private var activityPendingIntent: PendingIntent? = null
    private var activityRecognitionReceiver: BroadcastReceiver? = null

    private val isLocationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.fMap = supportFragmentManager.findFragmentById(R.id.f_map) as SupportMapFragment
        this.tvLocation = findViewById(R.id.tv_location)
        this.tvActivity = findViewById(R.id.tv_activity)

        this.locationHandler = LocationHandler(this)
        this.activityRecognitionHandler = ActivityRecognitionHandler(this)

        if (!isLocationPermissionGranted) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_ID)
        }
    }

    override fun onResume() {
        super.onResume()

        if (isLocationPermissionGranted) {
            fMap.getMapAsync(this)
            setupLocation()
            setupActivityRecognition()
        }
    }

    override fun onPause() {
        super.onPause()

        if (locationCallback != null) {
            locationHandler.unregisterLocationListener(locationCallback!!)
        }
        if (activityPendingIntent != null) {
            activityRecognitionHandler.unregisterPendingIntent(activityPendingIntent!!)
        }
        if (activityRecognitionReceiver != null) {
            unregisterReceiver(activityRecognitionReceiver)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.toast_location_permission, Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    private fun setupLocation() {
        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                val location = locationResult!!.lastLocation
                updateMap(location)
                updateLocationCard(location)
            }
        }
        locationHandler.registerLocationListener(locationCallback!!)
    }

    private fun setupActivityRecognition() {
        this.activityPendingIntent = activityRecognitionHandler.registerPendingIntent()

        this.activityRecognitionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Extract activity type from intent extras and pass it to updateActivityCard method.
                // Take a look at ActivityRecognitionService to see how intent extras are formed.

                updateActivityCard(
                    intent.getIntExtra(ActivityRecognitionService.ACTIVITY_EXTRA, 0)
                )

            }
        }

        // Register created receiver only for ActivityRecognitionService.INTENT_ACTION.
        registerReceiver(activityRecognitionReceiver, IntentFilter())
    }

    private fun updateMap(location: Location) {
        if (googleMap != null) {

            // Clear current marker and create a new marker based on the received location object.
                googleMap!!.clear()

                val newyork = LatLng(40.730610,-73.935242)
                googleMap!!.addMarker(
                    MarkerOptions().position(newyork).title("New York Marker")
                )

            // Use CameraUpdateFactory to perform a zoom in.
                googleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 20.toFloat()))
        }
    }

    private fun updateLocationCard(location: Location) {
        tvLocation.text = MessageFormat.format("Latitude: {0}\nLongitude: {1}\nAltitude: {2}",
                location.latitude, location.longitude, location.altitude)
    }

    private fun updateActivityCard(activityType: Int) {
        val activityResId: Int = when (activityType) {
            DetectedActivity.IN_VEHICLE -> R.string.activity_in_vehicle
            DetectedActivity.ON_BICYCLE -> R.string.activity_on_bicycle
            DetectedActivity.ON_FOOT -> R.string.activity_on_foot
            DetectedActivity.RUNNING -> R.string.activity_running
            DetectedActivity.WALKING -> R.string.activity_walking
            DetectedActivity.TILTING -> R.string.activity_tilting
            DetectedActivity.STILL -> R.string.activity_still
            else -> R.string.activity_unknown
        }

        tvActivity.text = String.format("%s: %s", getString(R.string.activity_title), getString(activityResId))
    }

    companion object {
        private const val PERMISSION_REQUEST_ID = 111
    }

}
