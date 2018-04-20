package org.andresoviedo.app.model3D.view;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.andresoviedo.app.model3D.SensorData;
import org.andresoviedo.app.model3D.SensorDevice;
import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.services.ExampleSceneLoader;
import org.andresoviedo.app.model3D.services.SceneLoader;
import org.andresoviedo.app.util.Utils;
import org.andresoviedo.app.util.content.ContentUtils;
import org.andresoviedo.dddmodel2.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Logging;

import bolts.Continuation;
import bolts.Task;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelActivity extends Activity implements ServiceConnection {

	private static final int REQUEST_CODE_OPEN_FILE = 1000;

	private String paramAssetDir;
	private String paramAssetFilename;
	/**
	 * The file to load. Passed as input parameter
	 */
	private String paramFilename;
	/**
	 * Enter into Android Immersive mode so the renderer is full screen or not
	 */
	private boolean immersiveMode = true;
	/**
	 * Background GL clear color. Default is light gray
	 */
	private float[] backgroundColor = new float[]{0.2f, 0.2f, 0.2f, 1.0f};

	private ModelSurfaceView gLView;

	private SceneLoader scene;

	private Handler handler;

	SensorDevice sensor1 = null;
	SensorDevice sensor2 = null;
	SensorDevice sensor3 = null;
	SensorDevice sensor4 = null;
	//private boolean boardReady= false;
	private static final String LOG_TAG_ACC = "Model1ActivityTagACC";
	private static final String LOG_TAG_GYRO = "Model1ActivityTagGYRO";
	private static final String LOG_TAG_SENSOR1 = "Sensor1";
	private static final String LOG_TAG_SENSOR2 = "Sensor2";
	private static final String LOG_TAG_SENSOR3 = "Sensor3";
	private static final String LOG_TAG_SENSOR4 = "Sensor4";
	private Debug debug;
	private Logging logging;

	BtleService.LocalBinder serviceBinder;
	BluetoothDevice btDevice1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Try to get input parameters
		Bundle b = getIntent().getExtras();
		if (b != null) {
			this.paramAssetDir = b.getString("assetDir");
			this.paramAssetFilename = b.getString("assetFilename");
			this.paramFilename = b.getString("uri");
			this.immersiveMode = "true".equalsIgnoreCase(b.getString("immersiveMode"));
			try{
				String[] backgroundColors = b.getString("backgroundColor").split(" ");
				backgroundColor[0] = Float.parseFloat(backgroundColors[0]);
				backgroundColor[1] = Float.parseFloat(backgroundColors[1]);
				backgroundColor[2] = Float.parseFloat(backgroundColors[2]);
				backgroundColor[3] = Float.parseFloat(backgroundColors[3]);
			}catch(Exception ex){
				// Assuming default background color
			}
		}
		Log.i("Renderer", "Params: assetDir '" + paramAssetDir + "', assetFilename '" + paramAssetFilename + "', uri '"
				+ paramFilename + "'");

		handler = new Handler(getMainLooper());

		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity.
		gLView = new ModelSurfaceView(this);
		setContentView(gLView);

		// Create our 3D sceneario
		if (paramFilename == null && paramAssetFilename == null) {
			scene = new ExampleSceneLoader(this);
		} else {
			scene = new SceneLoader(this);
		}

		scene.init();

		final Handler handlerTimer = new Handler();
		final int delay = 1000; //milliseconds
		/*handlerTimer.postDelayed(new Runnable(){
			public void run(){
				scene.replaceObject(0,
					Object3DBuilder.buildLine(
						new float[] {
							0.0f, 1.5f, 0.5f, 0.1f, 1.15f, 0.5f,
							0.1f, 1.15f, 0.5f, 0.1f, 0.75f, new Random().nextFloat()
						}
					).setColor(new float[] { 1.0f, 1.0f, 1.0f, 1.0f })
				);

				handlerTimer.postDelayed(this, delay);
			}
		}, delay);*/

		// Show the Up button in the action bar.
		setupActionBar();

		// TODO: Alert user when there is no multitouch support (2 fingers). He won't be able to rotate or zoom for
		// example
		Utils.printTouchCapabilities(getPackageManager());

		setupOnSystemVisibilityChangeListener();

		getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);
		//findViewById(R.id.btn_start).bringToFront();
		//findViewById(R.id.btn_stop).bringToFront();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.model, menu);
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupOnSystemVisibilityChangeListener() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			return;
		}
		getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				// Note that system bars will only be "visible" if none of the
				// LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
				if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
					// TODO: The system bars are visible. Make any desired
					// adjustments to your UI, such as showing the action bar or
					// other navigational controls.
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						hideSystemUIDelayed(3000);
					}
				} else {
					// TODO: The system bars are NOT visible. Make any desired
					// adjustments to your UI, such as hiding the action bar or
					// other navigational controls.
				}
			}
		});
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				if (immersiveMode) hideSystemUIDelayed(5000);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.model_toggle_wireframe:
				scene.toggleWireframe();
				break;
			case R.id.model_toggle_boundingbox:
				scene.toggleBoundingBox();
				break;
			case R.id.model_toggle_textures:
				scene.toggleTextures();
				break;
			case R.id.model_toggle_lights:
				scene.toggleLighting();
				break;
			case R.id.model_load_texture:
				Intent target = Utils.createGetContentIntent();
				Intent intent = Intent.createChooser(target, "Select a file");
				try {
					startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
				} catch (ActivityNotFoundException e) {
					// The reason for the existence of aFileChooser
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void hideSystemUIDelayed(long millis) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			return;
		}
		handler.postDelayed(new Runnable() {
			public void run() {
				hideSystemUI();
			}
		}, millis);
	}

	private void hideSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			hideSystemUIKitKat();
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			hideSystemUIJellyBean();
		}
	}

	// This snippet hides the system bars.
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void hideSystemUIKitKat() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			return;
		}
		// Set the IMMERSIVE flag.
		// Set the content to appear under the system bars so that the content
		// doesn't resize when the system bars hide and show.
		final View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void hideSystemUIJellyBean() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}
		final View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	// This snippet shows the system bars. It does this by removing all the flags
	// except for the ones that make the content appear under the system bars.
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void showSystemUI() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}
		final View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}

	public File getParamFile() {
		return getParamFilename() != null ? new File(getParamFilename()) : null;
	}

	public String getParamAssetDir() {
		return paramAssetDir;
	}

	public String getParamAssetFilename() {
		return paramAssetFilename;
	}

	public String getParamFilename() {
		return paramFilename;
	}

	public float[] getBackgroundColor(){
		return backgroundColor;
	}

	public SceneLoader getScene() {
		return scene;
	}

	public ModelSurfaceView getgLView() {
		return gLView;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CODE_OPEN_FILE:
				if (resultCode == RESULT_OK) {
					// The URI of the selected file
					final Uri uri = data.getData();
					Log.i("Menu", "Loading '" + uri.toString() + "'");
					if (uri != null) {
						final String path = ContentUtils.getPath(getApplicationContext(), uri);
						if (path != null) {
							try {
								scene.loadTexture(null, new URL("file://"+path));
							} catch (MalformedURLException e) {
								Toast.makeText(getApplicationContext(), "Problem loading texture '" + uri.toString() + "'",
										Toast.LENGTH_SHORT).show();
							}
						} else {
							Toast.makeText(getApplicationContext(), "Problem loading texture '" + uri.toString() + "'",
									Toast.LENGTH_SHORT).show();
						}
					}
				} else {
					Toast.makeText(getApplicationContext(), "Result when loading texture was '" + resultCode + "'",
							Toast.LENGTH_SHORT).show();
				}
		}
	}
	/*
	protected void boardReady() throws UnsupportedModuleException{
		accelerometer = mwBoard.getModuleOrThrow(Accelerometer.class);
		//fillRangeAdapter();
	}

	private void fillRangeAdapter() {
		ArrayAdapter<CharSequence> spinnerAdapter= null;
		if (accelerometer instanceof AccelerometerBosch) {
			spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_bmi160_acc_range, android.R.layout.simple_spinner_item);
		} else if (accelerometer instanceof AccelerometerMma8452q) {
			spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_mma8452q_acc_range, android.R.layout.simple_spinner_item);
		}

		if (spinnerAdapter != null) {
			spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			accRangeSelection.setAdapter(spinnerAdapter);
		}
	}

	private void unsupportedModule() {
		new AlertDialog.Builder(getActivity()).setTitle(R.string.title_error)
				.setMessage(String.format("%s %s", getContext().getString(sensorResId), getActivity().getString(R.string.error_unsupported_module)))
				.setCancelable(false)
				.setPositiveButton(R.string.label_ok, (dialog, id) -> enableDisableViewGroup((ViewGroup) getView(), false))
				.create()
				.show();
	}*/

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		serviceBinder = (BtleService.LocalBinder) service;
		BluetoothManager btManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

		sensor1 = new SensorDevice("C1:5F:8D:92:E5:07", btManager, serviceBinder, LOG_TAG_SENSOR1, this); ///< Put your board's MAC address here
		sensor2 = new SensorDevice("DE:C2:19:1D:A5:91", btManager, serviceBinder, LOG_TAG_SENSOR2, this); ///< Put your board's MAC address here
		//sensor3 = new SensorDevice("Put your board's MAC address here", btManager, serviceBinder, LOG_TAG_SENSOR3, this); ///< Put your board's MAC address here
		//sensor4 = new SensorDevice("Put your board's MAC address here", btManager, serviceBinder, LOG_TAG_SENSOR4, this); ///< Put your board's MAC address here
	}

	public Long lastTimestampSensor1 = Long.valueOf(0);
	public float[] lastQuaternionSensor1;
	public Long lastTimestampSensor2 = Long.valueOf(0);
	public float[] lastQuaternionSensor2;
	public Long lastTimestampSensor3 = Long.valueOf(0);
	public float[] lastQuaternionSensor3;
	public Long lastTimestampSensor4 = Long.valueOf(0);
	public float[] lastQuaternionSensor4;

	public void collectData(String sensorName, float[] Quaternion, Long timestampData)
	{
		if(scene != null) {
			if(sensorName == LOG_TAG_SENSOR1) {
				lastQuaternionSensor1 = Quaternion;
				lastTimestampSensor1 = timestampData;
			} else if(sensorName == LOG_TAG_SENSOR2) {
				lastQuaternionSensor2 = Quaternion;
				lastTimestampSensor2 = timestampData;
			} else if(sensorName == LOG_TAG_SENSOR3) {
				lastQuaternionSensor3 = Quaternion;
				lastTimestampSensor3 = timestampData;
			} else if(sensorName == LOG_TAG_SENSOR4) {
				lastQuaternionSensor4 = Quaternion;
				lastTimestampSensor4 = timestampData;
			}

			if( lastTimestampSensor1 != Long.valueOf(0) &&
				lastTimestampSensor2 != Long.valueOf(0))
				//lastTimestampSensor3 != Long.valueOf(0) &&
				//lastTimestampSensor4 != Long.valueOf(0))
			{
				//here hand is updated!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				scene.replaceObject(0,
						Object3DBuilder.buildLine(
								new float[]{
										0.0f, 1.5f, 0.5f, 0.1f, 1.15f, Quaternion[1],
										0.1f, 1.15f, Quaternion[1], 0.1f, 0.75f, Quaternion[0]
								}
						).setColor(new float[]{1.0f, 1.0f, 1.0f, 1.0f})
				);

			//rotate cube only when receiving from one sensor
			if(sensorName == LOG_TAG_SENSOR1) {
				String cameraViewVal = "Front";
				scene.replaceObject(1,
						Object3DBuilder.buildCubeForSensor(3f, 2f, 1f, 0f, 0f, 0f, cameraViewVal, Quaternion)
				);
			}
				String printText = Float.toString(lastQuaternionSensor1[0]) + " , " + Float.toString(lastQuaternionSensor1[1]) + " , " + Float.toString(lastQuaternionSensor1[2]) + " , " + Float.toString(lastQuaternionSensor1[3]) + " , ";
				printText += Float.toString(lastQuaternionSensor2[0]) + " , " + Float.toString(lastQuaternionSensor2[1]) + " , " + Float.toString(lastQuaternionSensor2[2]) + " , " + Float.toString(lastQuaternionSensor2[3]);
				Log.i("CombinedSensors", printText);

				lastTimestampSensor1 = Long.valueOf(0);
				lastTimestampSensor2 = Long.valueOf(0);
				lastTimestampSensor3 = Long.valueOf(0);
				lastTimestampSensor4 = Long.valueOf(0);
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		stopStreams();
	}

	@Override
	public void onBackPressed() {
		stopStreams();
		super.onBackPressed();
	}

	public void stopStreams()
	{
		if (sensor1 != null)
			sensor1.stopStreams();
		if (sensor2 != null)
			sensor2.stopStreams();
		if (sensor3 != null)
			sensor3.stopStreams();
		if (sensor4 != null)
			sensor4.stopStreams();

		if(logging != null){
			logging.stop();
			logging.downloadAsync().continueWith(new Continuation<Void, Void>() {
				@Override
				public Void then(Task<Void> task) throws Exception {
					Log.i(LOG_TAG_ACC, "Log download complete");
					return null;
				}
			});
		}
	}
}
