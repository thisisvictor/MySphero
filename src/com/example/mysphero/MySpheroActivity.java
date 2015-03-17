package com.example.mysphero;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import orbotix.robot.app.ColorPickerActivity;
import orbotix.robot.base.Robot;
import orbotix.robot.widgets.CalibrationImageButtonView;
import orbotix.robot.widgets.NoSpheroConnectedView;
import orbotix.robot.widgets.NoSpheroConnectedView.OnConnectButtonClickListener;
import orbotix.robot.widgets.SlideToSleepView;
import orbotix.robot.widgets.joystick.JoystickView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.calibration.ControllerActivity;
import orbotix.view.connection.SpheroConnectionView;

public class MySpheroActivity extends ControllerActivity {
    /** ID to start the StartupActivity for result to connect the Robot */
    private final static int STARTUP_ACTIVITY = 0;
    private static final int BLUETOOTH_ENABLE_REQUEST = 11;
    private static final int BLUETOOTH_SETTINGS_REQUEST = 12;

    /** ID to start the ColorPickerActivity for result to select a color */
    private final static int COLOR_PICKER_ACTIVITY = 1;
    private boolean mColorPickerShowing = false;

    /** The Robot to control */
    private Sphero mRobot;

    /** One-Touch Calibration Button */
    private CalibrationImageButtonView mCalibrationImageButtonView;

    /** Calibration View widget */
    private CalibrationView mCalibrationView;

    /** Slide to sleep view */
    private SlideToSleepView mSlideToSleepView;

    /** No Sphero Connected Pop-Up View */
    private NoSpheroConnectedView mNoSpheroConnectedView;

    /** Sphero Connection View */
    private SpheroConnectionView mSpheroConnectionView;

    //Colors
    private int mRed = 0xff;
    private int mGreen = 0xff;
    private int mBlue = 0xff;

    private BroadcastReceiver mColorChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update colors
            int red = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_RED, 0);
            int green = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, 0);
            int blue = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, 0);

            // change the color on the ball
            mRobot.setColor(red, green, blue);
        }
    };
    
    private boolean isMyoControlling = false;
    private boolean isMyoConnected = false;
    /** Listener class listening and responding to Myo events */
    private DeviceListener mListener = new AbstractDeviceListener() {
    	//called whenever a Myo has been connected
    	@Override
    	public void onConnect(Myo myo, long timestamp) {
    		Button myoButton = (Button)findViewById(R.id.myo_button);
    		myoButton.setBackgroundColor(Color.GREEN);
    		isMyoConnected = true;
    	}
    	
    	//called whenever a Myo has been disconnected
    	@Override
    	public void onDisconnect(Myo myo, long timestamp) {
    		Button myoButton = (Button)findViewById(R.id.myo_button);
    		myoButton.setBackgroundColor(Color.RED);
    		isMyoConnected = false;
    	}
    	
    	//called whenever Myo has recognized a sync gesture
    	@Override
    	public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) { }
    	
    	//called whenever Myo has detected itself being moved
    	@Override
    	public void onArmUnsync(Myo myo, long timestamp) { }
    	
    	//called whenever a synced Myo has been unlocked. Under the standard locking
    	// policy, that means poses will now be delivered to the listener.
    	@Override
    	public void onUnlock(Myo myo, long timestamp) { }
    	
    	//called whenever a synced Myo has been locked. Under the standard locking
    	// policy, that means poses will no longer be delivered to the listener.
    	@Override
    	public void onLock(Myo myo, long timestamp) { }
    	
    	//called whenever a Myo provides its current orientation, represented as a quaternion.
    	@Override
    	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
    		// Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
    		float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
    		float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
    		float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
    		// Adjust roll and pitch for the orientation of the Myo on the arm.
    		if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
    			roll *= -1;
    			pitch *= -1;
    		}
    		// Calculate the angle (and optional speed) of the Sphero
    		if(isMyoControlling && (pitch>5||pitch<-5)) {
    			float yAxis, xAxis; //the 2 parameters for the angle and speed
    			if(pitch<0) yAxis = -1*(float)Math.pow(pitch/90, 2);
    			else yAxis = (float)Math.pow(pitch/90, 2);
    			if(roll<0) xAxis = -1*(float)Math.pow(roll/180, 2);
    			else xAxis = (float)Math.pow(roll/180, 2);
    			float heading = (float)Math.toDegrees(Math.atan2(yAxis, xAxis));
    			float headingReal;
    			if(90-heading<0) headingReal = 90-heading+360;
    			else headingReal = 90-heading;
    			mRobot.drive(headingReal, 0.8f);
    		} else {
    			mRobot.stop();
    		}
    	}
    	
    	//called whenever a Myo provides a new pose.
    	@Override
    	public void onPose(Myo myo, long timestamp, Pose pose) {
    		// Handle the cases of the Pose enumeration, and change the text of the text view
    		// based on the pose we receive.
    		switch (pose) {
    			case UNKNOWN:
    				//mTextView.setText(getString(R.string.hello_world));
    				break;
    			case REST: 
    				isMyoControlling = false;
    				break;
    			case DOUBLE_TAP:
     				//int restTextId = R.string.hello_world;
    				switch (myo.getArm()) {
    					case LEFT:
    						//restTextId = R.string.arm_left;
    						break;
    					case RIGHT:
    						//restTextId = R.string.arm_right;
    						break;
    				}
    				//mTextView.setText(getString(restTextId));
    				break;
    			case FIST:
    				//mTextView.setText(getString(R.string.pose_fist));
    				isMyoControlling = true;
    				
    				break;
    			case WAVE_IN:
    				//mTextView.setText(getString(R.string.pose_wavein));
    				break;
    			case WAVE_OUT:
    				//mTextView.setText(getString(R.string.pose_waveout));
    				break;
    			case FINGERS_SPREAD:
    				//mTextView.setText(getString(R.string.pose_fingersspread));
    				break;
    		}
    		/* shouldn't be required if the unlock policy is set to none
    		if (pose != Pose.UNKNOWN && pose != Pose.REST) {
    			// Tell the Myo to stay unlocked until told otherwise. We do that here so you can
    			// hold the poses without the Myo becoming locked.
    			myo.unlock(Myo.UnlockType.HOLD);
    			// Notify the Myo that the pose has resulted in an action, in this case changing
    			// the text on the screen. The Myo will vibrate.
    			myo.notifyUserAction();
    		} else {
    			// Tell the Myo to stay unlocked only for a short period. This allows the Myo to
    			// stay unlocked while poses are being performed, but lock after inactivity.
    			myo.unlock(Myo.UnlockType.TIMED);
    		}*/
    	}
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        // Set up the Sphero Connection View
        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {

            @Override
            public void onConnected(Robot robot) {
                // Set Robot
                mRobot = (Sphero) robot; // safe to cast for now
                //Set connected Robot to the Controllers
                setRobot(mRobot);

                // Make sure you let the calibration view knows the robot it should control
                mCalibrationView.setRobot(mRobot);

                // Make connect sphero pop-up invisible if it was previously up
                mNoSpheroConnectedView.setVisibility(View.GONE);
                mNoSpheroConnectedView.switchToConnectButton();
            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                // let the SpheroConnectionView handle or hide it and do something here...
            }

            @Override
            public void onDisconnected(Robot sphero) {
                mSpheroConnectionView.startDiscovery();
            }
        });

        //Add the JoystickView as a Controller
        addController((JoystickView) findViewById(R.id.joystick));

        // Add the calibration view
        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

        // Set up sleep view
        mSlideToSleepView = (SlideToSleepView) findViewById(R.id.slide_to_sleep_view);
        mSlideToSleepView.hide();
        // Send ball to sleep after completed widget movement
        mSlideToSleepView.setOnSleepListener(new SlideToSleepView.OnSleepListener() {
            @Override
            public void onSleep() {
                mRobot.sleep(0);
            }
        });

        // Initialize calibrate button view where the calibration circle shows above button
        // This is the default behavior
        mCalibrationImageButtonView = (CalibrationImageButtonView) findViewById(R.id.calibration_image_button);
        mCalibrationImageButtonView.setCalibrationView(mCalibrationView);
        // You can also change the size and location of the calibration views (or you can set it in XML)
        mCalibrationImageButtonView.setRadius(100);
        mCalibrationImageButtonView.setOrientation(CalibrationView.CalibrationCircleLocation.ABOVE);

        // Grab the No Sphero Connected View
        mNoSpheroConnectedView = (NoSpheroConnectedView) findViewById(R.id.no_sphero_connected_view);
        mNoSpheroConnectedView.setOnConnectButtonClickListener(new OnConnectButtonClickListener() {

            @Override
            public void onConnectClick() {
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
            }

            @Override
            public void onSettingsClick() {
                // Open the Bluetooth Settings Intent
                Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                MySpheroActivity.this.startActivityForResult(settingsIntent, BLUETOOTH_SETTINGS_REQUEST);
            }
        });


        // Set up the Myo stuff
	     // First, we initialize the Hub singleton with an application identifier.
	     Hub hub = Hub.getInstance();
	     if (!hub.init(this, getPackageName())) {
	    	 // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
	    	 Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
	    	 finish();
	    	 return;
	     }
	     Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
	     // Next, register for DeviceListener callbacks.
	     hub.addListener(mListener);

    }


    /** Called when the user comes back to this app */
    @Override
    protected void onResume() {
        super.onResume();
        if (mColorPickerShowing) {
            mColorPickerShowing = false;
            return;
        }

        Log.d("", "registering Color Change Listener");
        IntentFilter filter = new IntentFilter(ColorPickerActivity.ACTION_COLOR_CHANGE);
        registerReceiver(mColorChangeReceiver, filter);
    }

    /** Called when the user presses the back or home button */
    @Override
    protected void onPause() {
        super.onPause();
        if (mColorPickerShowing) return;

        // Disconnect Robot properly
        if (mRobot != null) {
            mRobot.disconnect();
        }
        try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// We don't want any callbacks when the Activity is gone, so unregister the listener.
    	Hub.getInstance().removeListener(mListener);
    	if (isFinishing()) {
    		// The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
    		Hub.getInstance().shutdown();
    	}
        // Disconnect Robot properly
        if (mRobot != null) {
            mRobot.disconnect();
        }
        try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == COLOR_PICKER_ACTIVITY) {
                //Get the colors
                mRed = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_RED, 0xff);
                mGreen = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, 0xff);
                mBlue = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, 0xff);

                //Set the color
                mRobot.setColor(mRed, mGreen, mBlue);
            } else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {
                // User enabled bluetooth, so refresh Sphero list
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
            }
        } else {
            if (requestCode == STARTUP_ACTIVITY) {
                // Failed to return any robot, so we bring up the no robot connected view
                mNoSpheroConnectedView.setVisibility(View.VISIBLE);
            } else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {

                // User clicked "NO" on bluetooth enable settings screen
                Toast.makeText(MySpheroActivity.this,
                        "Enable Bluetooth to Connect to Sphero", Toast.LENGTH_LONG).show();
            } else if (requestCode == BLUETOOTH_SETTINGS_REQUEST) {
                // User enabled bluetooth, so refresh Sphero list
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
            }
        }
    }

    /**
     * When the user clicks the "Color" button, show the ColorPickerActivity
     *
     * @param v The Button clicked
     */
    public void onColorClick(View v) {

        mColorPickerShowing = true;
        Intent i = new Intent(this, ColorPickerActivity.class);

        //Tell the ColorPickerActivity which color to have the cursor on.
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_RED, mRed);
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, mGreen);
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, mBlue);

        startActivityForResult(i, COLOR_PICKER_ACTIVITY);
    }

    /**
     * When the user clicks the "Sleep" button, show the SlideToSleepView shows
     *
     * @param v The Button clicked
     */
    public void onSleepClick(View v) {
        mSlideToSleepView.show();
    }
    
    /**
     * When the user clicks the "Myo" button, call up the built-in activity
     * for scanning and connecting to particular Myo devices
     */
    public void onMyoClick(View v) {
    	if(isMyoConnected) return; //skip the rest if a Myo is already connected to
    	//Toast.makeText(this, "Connect to a Myo", Toast.LENGTH_SHORT).show();
    	// Launch the ScanActivity to scan for Myos to connect to.
    	//Intent intent = new Intent(this, ScanActivity.class);
    	//startActivity(intent);
    	Hub.getInstance().attachToAdjacentMyo();
    	//turn off the mSpheroConnectionView
    	mSpheroConnectionView.setVisibility(View.GONE);
    	mSpheroConnectionView.clearListeners();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        mSlideToSleepView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }
}
