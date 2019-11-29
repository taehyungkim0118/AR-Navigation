package com.example.tmaptest;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tmaptest.Utils.DemoUtils;
import com.example.tmaptest.Utils.ScreenUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback, View.OnClickListener, SensorEventListener {

    // TMap variable
    private static final int REQUEST_SEARCH = 0x0001;
    private static final int REQUEST_CAMERA_PERMISSION = 1234;

    private static final int REQUSET_AR_ON = 0x0101;
    private static final int REQUEST_AR_OFF = 0x0102;

    private TMapView tMapView;
    private FrameLayout frameLayoutTmap;

    private Context mContext = null;
    private boolean m_bTrackingMode = true;

    private TMapGpsManager tmapGPS = null;
    private static int mMarkerID;
    private GpsTracker currentLocationPoint;

    private ArrayList<String> mArrayMarkerID = new ArrayList<String>();
    private ArrayList<MapPoint> m_mapPoint = new ArrayList<MapPoint>();


    private Button bt_find;
    private Button bt_fac;
    private Button bt_startAR;
    private Button bt_main;
    private Button bt_changeGPSMode;

    private boolean isGpsModeIn = false;

    private FloatingActionButton fab_gps;

    private ArrayList mapPoint = new ArrayList();

    private int size = 0;

    //     ARCore variable
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable blue_Renderable;
    private ModelRenderable pointerRenderable;
    private ModelRenderable arrow_Renderable;
    private ModelRenderable arrow2_Renderable;

    private Node layoutNode = null;

    private double distance;


    private LinearLayout arVisibility;

    private int EWSN = 0; // East = 1, West = 2, South = 3, North = 4

    int i = 0; // map Point 를 변경

    private ViewRenderable exampleLayoutRenderable;

    boolean firstSet = true;

    // gps 방위각
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAcclerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAcclerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float degree;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // gps 방위각
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        // ARCore Check
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        /**
         *  TMap
         */
        try {
            mContext = this;

            TMapData tMapData = new TMapData();

            bt_find = (Button) findViewById(R.id.bt_findadd);
            bt_fac = (Button) findViewById(R.id.bt_findfac);
            fab_gps = (FloatingActionButton) findViewById(R.id.btn_gps);
            bt_startAR = (Button) findViewById(R.id.bt_startAR);
            bt_main = (Button) findViewById(R.id.bt_main);
            arVisibility = (LinearLayout) findViewById(R.id.arVisibility);
            bt_changeGPSMode = (Button) findViewById(R.id.bt_in_or_out);

            bt_find.setOnClickListener(this);
            bt_fac.setOnClickListener(this);
            fab_gps.setOnClickListener(this);
            bt_startAR.setOnClickListener(this);
            bt_main.setOnClickListener(this);
            bt_changeGPSMode.setOnClickListener(this);

            currentLocationPoint = new GpsTracker(this);
            currentLocationPoint.getLocation();
            initialViews();

        } catch (Exception e) {

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_findadd:
                Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(searchIntent, REQUEST_SEARCH);
                break;

            case R.id.bt_findfac:
                try {
                    getAroundBizPoi();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.btn_gps:
                tMapView.setCenterPoint(currentLocationPoint.getLongitude(), currentLocationPoint.getLatitude());
                tMapView.setCompassMode(true);
                break;

            case R.id.bt_startAR:
                changeMapSize(REQUSET_AR_ON);
                arVisibility.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int frameStack = 0;
                        frameStack++;
                        Log.d("frameStack", "frameStack" + frameStack);
                        initARCore();
                    }
                }).run();
                break;

            case R.id.bt_main:
                changeMapSize(REQUEST_AR_OFF);
                arVisibility.setVisibility(View.INVISIBLE);
//                arFragment.onDestroyView();
                break;

            case R.id.bt_in_or_out:
                if (isGpsModeIn) { // gps 실외 기능 지원
                    tmapGPS.setProvider(tmapGPS.GPS_PROVIDER); // gps로 현 위치를 받음
                    isGpsModeIn = false;
                    bt_changeGPSMode.setText("GPS : 실외");
                } else { // gps 실내 기능 지원
                    tmapGPS.setProvider(tmapGPS.NETWORK_PROVIDER); // 연결된 인터넷으로 현 위치를 받음(실내에서 유용)
                    isGpsModeIn = true;
                    bt_changeGPSMode.setText("GPS : 실내");
                }
        }
    }

    private int frameCheck = 0;
    private int modelStack = 0;

    void timer() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                frameCheck++;
                Log.d("frameCheck", "frameCheck : " + frameCheck);
            }
        };

        timer.schedule(timerTask, 0, 1000);
    }


    /**
     * **********************************************************************************************
     * ************************************** ARCore Method *****************************************
     * **********************************************************************************************
     */
    void initARCore() {
        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> exampleLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.back_layout)
                        .build();
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ModelRenderable> blue_marker = ModelRenderable.builder()
                .setSource(this, R.raw.blue_marker)
                .build();

        CompletableFuture<ModelRenderable> destination = ModelRenderable.builder()
                .setSource(this, R.raw.map_pointer)
                .build();

        CompletableFuture<ModelRenderable> arrow = ModelRenderable.builder()
                .setSource(this, R.raw.arrow)
                .build();

        CompletableFuture<ModelRenderable> arrow2 = ModelRenderable.builder()
                .setSource(this, R.raw.arrow2)
                .build();

        CompletableFuture.allOf(
                exampleLayout,
                blue_marker,
                destination,
                arrow,
                arrow2)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                exampleLayoutRenderable = exampleLayout.get();
                                blue_Renderable = blue_marker.get();
                                pointerRenderable = destination.get();
//                                arrow_Renderable = arrow.get();
//                                arrow2_Renderable = arrow2.get();

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

        // 현재 가리키는 방향 감지
        if (degree >= 45 && degree < 135) { // 동
            EWSN = 1;
        } else if (degree >= 225 && degree < 315) { // 서
            EWSN = 2;
        } else if (degree >= 135 && degree < 225) { // 남
            EWSN = 3;
        } else if (degree >= 315 || degree < 45) { // 북
            EWSN = 4;
        }

        timer();

        arFragment
                .getArSceneView()
                .getScene()
                .addOnUpdateListener(frameTime -> {
                            if (frameCheck > 3 || firstSet) {
                                firstSet = false;

                                try {
                                    //get the frame from the scene for shorthand
                                    Frame frame = arFragment.getArSceneView().getArFrame();
                                    if (frame != null) {
                                        Log.d("addOnUpdateListener frame", "addOnUpdateListener frame");
                                        //get the trackables to ensure planes are detected
                                        Iterator<Plane> var3 = frame.getUpdatedTrackables(Plane.class).iterator();
                                        while (var3.hasNext()) {
                                            Log.d("addOnUpdateListener var3.hasNext()", "addOnUpdateListener var3.hasNext()");
                                            Plane plane = var3.next();

                                            //If a plane has been detected & is being tracked by ARCore
                                            if (plane.getTrackingState() == TrackingState.TRACKING) {
                                                Log.d("addOnUpdateListener TRACKING", "addOnUpdateListener TRACKING");

                                                //Hide the plane discovery helper animation
                                                arFragment.getPlaneDiscoveryController().hide();

                                                //Get all added anchors to the frame
                                                Iterator<Anchor> iterableAnchor = frame.getUpdatedAnchors().iterator();
                                                Log.d("addOnUpdateListener iterableAnchor", "iterableAnchor : " + iterableAnchor.hasNext());
                                                //place the first object only if no previous anchors were added
                                                if (!iterableAnchor.hasNext()) {
                                                    Log.d("addOnUpdateListener iterableAnchor", "addOnUpdateListener iterableAnchor");
                                                    //Perform a hit test at the center of the screen to place an object without tapping

                                                    List<HitResult> hitTest = frame.hitTest(screenCenter().x, screenCenter().y);

                                                    //iterate through all hits
                                                    Iterator<HitResult> hitTestIterator = hitTest.iterator();
                                                    while (hitTestIterator.hasNext()) {

                                                        Log.d("addOnUpdateListener hitTestIterator", "addOnUpdateListener hitTestIterator");
                                                        HitResult hitResult = hitTestIterator.next();

                                                        //Create an anchor at the plane hit
                                                        Anchor modelAnchor = plane.createAnchor(hitResult.getHitPose());

                                                        //Attach a node to this anchor with the scene as the parent
                                                        AnchorNode anchorNode = new AnchorNode(modelAnchor);
                                                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                                                        if (modelStack > 0) {
                                                            List<Node> nodeList = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
                                                            for (Node childNode : nodeList) {
                                                                if (childNode instanceof AnchorNode) {
                                                                    if (((AnchorNode) childNode).getAnchor() != null) {
                                                                        ((AnchorNode) childNode).getAnchor().detach();
                                                                        ((AnchorNode) childNode).setParent(null);
                                                                    }
                                                                }
                                                            }

                                                            modelStack = 0;
                                                            Log.d("modelStack", "is remove ? modelStack : " + modelStack);
                                                        }

                                                        anchorNode = new AnchorNode(modelAnchor);
                                                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                                                        frameCheck = 0;

                                                        // 목적지로 가는 경로의 포인트와 현재위치가 일정 거리로 가까워지면 다음 포인트로 안내
                                                        if (currentLocationPoint.getLatitude() > getPointLatitude(i) - 0.00034 && currentLocationPoint.getLatitude() < getPointLatitude(i) + 0.00034
                                                                && currentLocationPoint.getLongitude() > getPointLogitude(i) - 0.00034 && currentLocationPoint.getLongitude() < getPointLogitude(i) + 0.00034) {
                                                            i++;
                                                            if (size != 0 && size < i) {
                                                                i = size;
                                                            }
                                                        }

                                                        Log.d("현재위치", "현재위치 Lat : " + currentLocationPoint.getLatitude() + " Lon : " + currentLocationPoint.getLongitude());
                                                        Log.d("포인트 위치", "포인트 Lat : " + getPointLatitude(i) + " Lon : " + getPointLogitude(i));
                                                        Log.d("포인트 값", "포인트 i = " + i);


                                                        // 현재위치 기준으로 경로의 포인트를 방위로 계산
                                                        // 동쪽 1,4 사분면
                                                        if(i == size) {
                                                            if(layoutNode != null) {
                                                                layoutNode.setParent(null);
                                                            }
                                                            Anchor anchor = hitResult.createAnchor();
                                                            AnchorNode anchorNode2 = new AnchorNode(anchor);
                                                            anchorNode2.setParent(arFragment.getArSceneView().getScene());

                                                            layoutNode = new Node();
                                                            layoutNode.setParent(anchorNode2);
                                                            layoutNode.setRenderable(exampleLayoutRenderable);
                                                            View eView = exampleLayoutRenderable.getView();
                                                            TextView textAR = eView.findViewById(R.id.textView);
                                                            textAR.setText("도착");
                                                            Vector3 vector3 = new Vector3(anchor.getPose().tx(),
                                                                    anchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                                                                    anchor.getPose().tz());
                                                            layoutNode.setWorldPosition(vector3);

                                                            break;
                                                        }

                                                        if (currentLocationPoint.getLongitude() + 0.00034 < getPointLogitude(i)) {
                                                            // 1 사분면
                                                            Log.d("1사분면", "EWSN : " + EWSN);
                                                            if (currentLocationPoint.getLatitude() + 0.00034 < getPointLatitude(i)) {
                                                                if (EWSN == 1) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -45.0f);
                                                                } else if (EWSN == 2) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 135.0f);
                                                                } else if (EWSN == 3) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -135.0f);
                                                                } else if (EWSN == 4) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 45.0f);
                                                                }
                                                            }
                                                            // 4 사분면
                                                            else if (currentLocationPoint.getLatitude() - 0.00034 > getPointLatitude(i)) {
                                                                Log.d("4사분면", "EWSN : " + EWSN);
                                                                if (EWSN == 1) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -45.0f); // 1사분면
                                                                } else if (EWSN == 2) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 135.0f); // 3사분면
                                                                } else if (EWSN == 3) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 45.0f); // 2사분면
                                                                } else if (EWSN == 4) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -135.0f); // 4사분면
                                                                }
                                                            }
                                                            // 동쪽
                                                            else {
                                                                Log.d("동쪽", "EWSN : " + EWSN);
                                                                if (EWSN == 1) {
                                                                    straightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 2) {
                                                                    backModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 3) {
                                                                    leftModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 4) {
                                                                    rightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                }
                                                            }
                                                        }
                                                        // 현재위치 기준 2,3 사분면
                                                        else if (currentLocationPoint.getLongitude() - 0.00034 > getPointLogitude(i)) {
                                                            Log.d("2사분면", "EWSN : " + EWSN);
                                                            // 2 사분면
                                                            if (currentLocationPoint.getLatitude() + 0.00034 < getPointLatitude(i)) {
                                                                if (EWSN == 1) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 135.0f);
                                                                } else if (EWSN == 2) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -45.0f);
                                                                } else if (EWSN == 3) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -135.0f);
                                                                } else if (EWSN == 4) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 45.0f);
                                                                }
                                                            }
                                                            // 3 사분면
                                                            else if (currentLocationPoint.getLatitude() - 0.00034 > getPointLatitude(i)) {
                                                                Log.d("3사분면", "EWSN : " + EWSN);
                                                                if (EWSN == 1) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -135.0f);
                                                                } else if (EWSN == 2) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 45.0f);
                                                                } else if (EWSN == 3) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, -45.0f);
                                                                } else if (EWSN == 4) {
                                                                    modelSet(arFragment, anchorNode, modelAnchor, blue_Renderable, 135.0f);
                                                                }
                                                            }
                                                            // 서쪽
                                                            else {
                                                                Log.d("서쪽", "EWSN : " + EWSN);
                                                                if (EWSN == 1) {
                                                                    backModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 2) {
                                                                    straightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 3) {
                                                                    rightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 4) {
                                                                    leftModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                }
                                                            }

                                                        } else {
                                                            Log.d("북쪽", "EWSN : " + EWSN);
                                                            // 북쪽
                                                            if (currentLocationPoint.getLatitude() + 0.00034 < getPointLatitude(i)) {
                                                                if (EWSN == 1) {
                                                                    leftModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 2) {
                                                                    rightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 3) {
                                                                    backModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 4) {
                                                                    straightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                }
                                                            }
                                                            // 남쪽
                                                            else if (currentLocationPoint.getLatitude() - 0.00034 > getPointLatitude(i)) {
                                                                Log.d("남쪽", "EWSN : " + EWSN);
                                                                if (EWSN == 1) {
                                                                    rightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 2) {
                                                                    leftModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 3) {
                                                                    straightModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                } else if (EWSN == 4) {
                                                                    backModel(arFragment, anchorNode, modelAnchor, blue_Renderable);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.d("Exception", "Exception : " + e.toString());
                                }
                            }
                        }
                );

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if(layoutNode != null) {
                        layoutNode.setParent(null);
                    }
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    layoutNode = new Node();
                    layoutNode.setParent(anchorNode);
                    layoutNode.setRenderable(exampleLayoutRenderable);
                    View eView = exampleLayoutRenderable.getView();
                    TextView textAR = eView.findViewById(R.id.textView);
                    textAR.setText("거리 : " + distance + " m");
                    Vector3 vector3 = new Vector3(anchor.getPose().tx(),
                            anchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                            anchor.getPose().tz());
                    layoutNode.setWorldPosition(vector3);
                }
        );
    }

    public void modelSet(ArFragment arFragment, AnchorNode anchorNode, Anchor modelAnchor, ModelRenderable arrow_Renderable, float angle) {
        //create a new TranformableNode that will carry our object
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // model resize
        transformableNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        // right model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), angle + 180));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(arrow_Renderable);

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        Vector3 vector3 = new Vector3(modelAnchor.getPose().tx(),
                modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                modelAnchor.getPose().tz());
        transformableNode.setWorldPosition(vector3);
        modelStack++;
    }


    public void rightModel(ArFragment arFragment, AnchorNode anchorNode, Anchor modelAnchor, ModelRenderable arrow_Renderable) {
        //create a new TranformableNode that will carry our object
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // model resize
        transformableNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        // right model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 90.0f));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(arrow_Renderable);

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        Vector3 vector3 = new Vector3(modelAnchor.getPose().tx(),
                modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                modelAnchor.getPose().tz());
        transformableNode.setWorldPosition(vector3);
        modelStack++;
        Log.d("rightmodel", "rightmodel setting");
    }

    public void leftModel(ArFragment arFragment, AnchorNode anchorNode, Anchor modelAnchor, ModelRenderable arrow_Renderable) {
        //create a new TranformableNode that will carry our object
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // model resize
        transformableNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        // left model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), -90.0f));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(arrow_Renderable);

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        Vector3 vector3 = new Vector3(modelAnchor.getPose().tx(),
                modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                modelAnchor.getPose().tz());
        transformableNode.setWorldPosition(vector3);
        modelStack++;
        Log.d("leftModel", "leftModel setting");
    }

    public void straightModel(ArFragment arFragment, AnchorNode anchorNode, Anchor modelAnchor, ModelRenderable arrow2_Renderable) {
        //create a new TranformableNode that will carry our object
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // model resize
        transformableNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        // straight model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 180.0f));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(arrow2_Renderable);

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        Vector3 vector3 = new Vector3(modelAnchor.getPose().tx(),
                modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                modelAnchor.getPose().tz());
        transformableNode.setWorldPosition(vector3);
        modelStack++;
        Log.d("straightModel", "straightModel setting");
    }

    public void backModel(ArFragment arFragment, AnchorNode anchorNode, Anchor modelAnchor, ModelRenderable arrow2_Renderable) {
        //create a new TranformableNode that will carry our object
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // model resize
        transformableNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        // back model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(.0f, 0.0f, 0.0f), 0.0f));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(arrow2_Renderable);

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        Vector3 vector3 = new Vector3(modelAnchor.getPose().tx(),
                modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                modelAnchor.getPose().tz());
        transformableNode.setWorldPosition(vector3);
        modelStack++;
        Log.d("backModel", "backModel setting");
    }

    public Vector3 screenCenter() {
        float screenHeight = ScreenUtils.getScreenHeight(this);
        float screenWidth = ScreenUtils.getScreenWidth(this);

        return new Vector3(screenWidth / 2f, screenHeight / 2f, 0f);
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    /**
     * Example node of a layout
     */
    private Node getExampleView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();

        Vector3 world = new Vector3();
        base.setWorldScale(world);

        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched... Local : " + base.getLocalScale() + " World : " + base.getWorldScale(), Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
    }

    /***
     * Example Node of a 3D model
     */
    private Node getBlueMarker() {
        Node base = new Node();
        base.setRenderable(blue_Renderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "BlueMarker touched... Local : " + base.getLocalScale() + " World : " + base.getWorldScale(), Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    private Node getPointerMarker() {
        Node base = new Node();
        base.setRenderable(pointerRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Destination touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    /**
     * **********************************************************************************************
     * ************************************ TMap Method *********************************************
     * **********************************************************************************************
     */
    private void initialViews() {
        try {
            // Map On
            tMapView = new TMapView(mContext);
            frameLayoutTmap = (FrameLayout) findViewById(R.id.frameLayoutTmap);
            tMapView.setSKTMapApiKey(getString(R.string.tmap_api_key));
            frameLayoutTmap.addView(tMapView);
            Log.d("tMapView", "tMapView");

            tMapView.setLocationPoint(currentLocationPoint.getLongitude(), currentLocationPoint.getLatitude()); // 현재위치로 표시될 자표의 위도, 경도 설정
            tMapView.setCenterPoint(currentLocationPoint.getLongitude(), currentLocationPoint.getLatitude()); // 지도의 중심좌표를 이동

//            addPoint();
//            showMarkerPoint();

            tMapView.setCompassMode(false); // gps 방향대로 지도가 움직임
            tMapView.setIconVisibility(true); // 현재위치로 표시될 아이콘을 표시할지 여부를 설정
            tMapView.setZoomLevel(15); // level 7~19

            tmapGPS = new TMapGpsManager(MainActivity.this);
            tmapGPS.setMinTime(1000);
//            tmapGPS.setProvider(tmapGPS.NETWORK_PROVIDER); // 연결된 인터넷으로 현 위치를 받음(실내에서 유용)
            tmapGPS.setProvider(tmapGPS.GPS_PROVIDER); // gps로 현 위치를 받음
            tmapGPS.OpenGps();

            tMapView.setTrackingMode(true);
            tMapView.setSightVisible(true);

        } catch (Exception e) {
            Log.d("Exception:", "can't initialize. " + e.getMessage());
        }

    }

    void navigation(double latitude, double longitude) {
        TMapData mapData = new TMapData();
        TMapPoint startpoint = new TMapPoint(currentLocationPoint.getLatitude(), currentLocationPoint.getLongitude());
        TMapPoint destination = new TMapPoint(latitude, longitude);

        mapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startpoint, destination, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                mapPoint = tMapPolyLine.getLinePoint();
                size = mapPoint.size();
                for (int i = 0; i < size; i++) {
                    Log.d("map Point " + i, " Latitude : " + mapPoint.get(i).toString().substring(mapPoint.get(i).toString().lastIndexOf("t") + 2, mapPoint.get(i).toString().lastIndexOf("o") - 2) +
                            "Longitude : " + mapPoint.get(i).toString().substring(mapPoint.get(i).toString().lastIndexOf("n") + 2));
                }
                tMapView.addTMapPath(tMapPolyLine);
//                distanceView = (TextView) findViewById(R.id.distance);
//                distanceView.setText("거리 : " + Math.round(tMapPolyLine.getDistance() * 100) / 100.0 + " m");
                distance = Math.round(tMapPolyLine.getDistance() * 100) / 100.0;

            }
        });
    }

    public float getPointLogitude(int i) {
        float pointLogitude = Float.parseFloat(mapPoint.get(i).toString().substring(mapPoint.get(i).toString().lastIndexOf("n") + 2));
        return pointLogitude;
    }

    public float getPointLatitude(int i) {
        float pointLatitude = Float.parseFloat(mapPoint.get(i).toString().substring(mapPoint.get(i).toString().lastIndexOf("t") + 2, mapPoint.get(i).toString().lastIndexOf("o") - 2));
        return pointLatitude;
    }

    public void getAroundBizPoi() {
        TMapData tMapData = new TMapData();

        TMapPoint point = tMapView.getCenterPoint();

        tMapData.findAroundNamePOI(point, "편의점;은행", 1, 99,
                new TMapData.FindAroundNamePOIListenerCallback() {
                    @Override
                    public void onFindAroundNamePOI(ArrayList<TMapPOIItem> arrayList) {
                        for (int i = 0; i < arrayList.size(); i++) {
                            TMapPOIItem item = arrayList.get(i);
                            Log.d("편의시설", "POI Name: " + item.getPOIName() + ", " +
                                    "Address: " + item.getPOIAddress().replace("null", ""));
                            TMapMarkerItem markerItem = new TMapMarkerItem();
                            Bitmap bitmap = null;
                            bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_pin_marker);

                            markerItem.setTMapPoint(item.getPOIPoint());
                            markerItem.setName(item.getPOIName());
                            markerItem.setVisible(markerItem.VISIBLE);

                            markerItem.setIcon(bitmap);

                            markerItem.setCalloutTitle(item.getPOIName());
                            markerItem.setCalloutSubTitle(item.getPOIAddress().replace("null",""));
                            markerItem.setCanShowCallout(true);
                            markerItem.setAutoCalloutVisible(true);

                            Bitmap bitmap_i = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_bubble);

                            markerItem.setCalloutRightButtonImage(bitmap_i);

                            String strID = String.format("pmarker%d", mMarkerID++);

                            tMapView.addMarkerItem(strID, markerItem);
                            mArrayMarkerID.add(strID);
                        }
                    }
                });
    }

    public void addPoint() { // 핀 포인트를 배열에 add
        // 한경대학교
        m_mapPoint.add(new MapPoint("한경대학교", 37.011758, 127.264226));
    }

    public void showMarkerPoint() {
        for (int i = 0; i < m_mapPoint.size(); i++) {
            TMapPoint point = new TMapPoint(m_mapPoint.get(i).getLatitude(), m_mapPoint.get(i).getLongitude());
            TMapMarkerItem item = new TMapMarkerItem();
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_pin_marker);

            item.setTMapPoint(point);
            item.setName(m_mapPoint.get(i).getName());
            item.setVisible(item.VISIBLE);

            item.setIcon(bitmap);

            // 풍선뷰 안 항목에 글을 지정
            item.setCalloutTitle(m_mapPoint.get(i).getName());
            item.setCalloutSubTitle("서브");
            item.setCanShowCallout(true);
            item.setAutoCalloutVisible(true);

            Bitmap bitmap_i = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_bubble);

            item.setCalloutRightButtonImage(bitmap_i);

            String strID = String.format("pmarker%d", mMarkerID++);

            tMapView.addMarkerItem(strID, item);
            mArrayMarkerID.add(strID);
        }
    }

    /* 맵 사이즈 축소 함수 */
    void changeMapSize(int requestCode) {
        if (requestCode == REQUSET_AR_ON) {
            float screenHeight = ScreenUtils.getScreenHeight(this);
            float screenWidth = ScreenUtils.getScreenWidth(this);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (screenWidth / 1.7), (int) (screenHeight / 3.5));
            params.setMargins(0, 0, 16, 16);
            params.setMargins(0, 0, 20, 20);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            frameLayoutTmap.setLayoutParams(params);

            tMapView.setCenterPoint(currentLocationPoint.getLongitude(), currentLocationPoint.getLatitude());
            tMapView.setCompassMode(true);
            tMapView.setZoomLevel(18);
        } else if (requestCode == REQUEST_AR_OFF) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            frameLayoutTmap.setLayoutParams(params);
        }
    }


    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    protected void onPause() {
        super.onPause();

        // 방위각
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);

        arFragment.getArSceneView().pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            tmapGPS.CloseGps();
        } catch (Exception e) {

        }
        arFragment.getArSceneView().destroy();
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 방위각
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);


//        if (arFragment.getArSceneView().getSession() == null) {
//            // If the session wasn't created yet, don't resume rendering.
//            // This can happen if ARCore needs to be updated or permissions are not granted yet.
//            try {
//                Session session = DemoUtils.createArSession(this, installRequested);
//                if (session == null) {
//                    installRequested = ARLocationPermissionHelper.hasPermission(this);
//                    return;
//                } else {
//                    arFragment.getArSceneView().setupSession(session);
//                }
//            } catch (UnavailableException e) {
//                DemoUtils.handleSessionException(this, e);
//            }
//        }
//
//        try {
//            arFragment.getArSceneView().resume();
//        } catch (CameraNotAvailableException ex) {
//            DemoUtils.displayError(this, "Unable to get camera", ex);
//            finish();
//            return;
//        }
//
//        if (arFragment.getArSceneView().getSession() != null) {
//            showLoadingMessage();
//        }
    }

    /*
      위치가 바뀔때마다 생성
     */
    @Override
    public void onLocationChange(Location location) {
        if (m_bTrackingMode) {
            currentLocationPoint.getLocation();
            tMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
        }
    }

    /**
     * 1. Tmap 좌표값 전달
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SEARCH: {
                    String name = data.getStringExtra("POI");
                    double longitude = data.getDoubleExtra("LON", 0.0);
                    double latitude = data.getDoubleExtra("LAT", 0.0);
                    navigation(latitude, longitude);
                    break;
                }
                default: {
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 윈도우의 포커스가 변경되었을 경우 호출
     * 앱 실행 : TRUE
     * 포커스 잃을 경우 : FALSE
     *
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /**
     * 센서가 바뀔때마다 실행되는 메서드
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAcclerometer, 0, event.values.length);
            mLastAcclerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAcclerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAcclerometer, mLastMagnetometer);
            float azimuthinDegree = (int) (Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0]) + 360) % 360;
            degree = azimuthinDegree;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
