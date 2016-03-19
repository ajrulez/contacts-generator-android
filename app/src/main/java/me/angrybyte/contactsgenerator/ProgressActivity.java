
package me.angrybyte.contactsgenerator;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import me.angrybyte.contactsgenerator.api.Gender;
import me.angrybyte.contactsgenerator.parser.data.Person;
import me.angrybyte.contactsgenerator.service.GeneratorService;
import me.angrybyte.contactsgenerator.service.GeneratorServiceBinder;
import me.angrybyte.contactsgenerator.service.OnGenerateProgressListener;
import me.angrybyte.contactsgenerator.service.OnGenerateResultListener;
import me.angrybyte.contactsgenerator.service.ServiceApi;

public class ProgressActivity extends AppCompatActivity implements ServiceConnection, OnGenerateProgressListener, OnGenerateResultListener,
        View.OnClickListener {

    public static final String TAG = ProgressActivity.class.getSimpleName();
    private static final int[] AVATARS = new int[] {
            R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6,
            R.drawable.avatar_7, R.drawable.avatar_8, R.drawable.avatar_9, R.drawable.avatar_10, R.drawable.avatar_11,
            R.drawable.avatar_12, R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16,
            R.drawable.avatar_17, R.drawable.avatar_18, R.drawable.avatar_19, R.drawable.avatar_20, R.drawable.avatar_21,
            R.drawable.avatar_22, R.drawable.avatar_23, R.drawable.avatar_24, R.drawable.avatar_25, R.drawable.avatar_26,
            R.drawable.avatar_27, R.drawable.avatar_28, R.drawable.avatar_29, R.drawable.avatar_30
    };

    public static final String KEY_NUMBER = "KEY_NUMBER";
    public static final String KEY_IMAGES = "KEY_IMAGES";
    public static final String KEY_GENDER = "KEY_GENDER";

    private ServiceApi mService;

    @Gender
    private String mGender;
    private boolean mFetchImages;
    private int mRequestedNumber;

    private ViewGroup mContactInfoView;
    private TextSwitcher mActivityTitle;
    private TextView mContactDisplayNameView;
    private TextView mContactPhoneNumberView;
    private TextView mContactEmailView;
    private ImageView mContactPhotoView;
    private ProgressBar mProgressBar;
    private Button mStopButton;

    private String mHeader;
    private long mAnimationLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        readIntentData();
        assignViews();
        performInitialSetup();
    }

    private void readIntentData() {
        mRequestedNumber = getIntent().getIntExtra(KEY_NUMBER, 1);
        mFetchImages = getIntent().getBooleanExtra(KEY_IMAGES, false);
        // noinspection WrongConstant
        mGender = getIntent().getStringExtra(KEY_GENDER);
        Log.d(TAG, "Received request for " + mRequestedNumber + " contacts " + (mFetchImages ? "with " : "without ") + "pictures. Gender: "
                + mGender);
    }

    private void assignViews() {
        mContactInfoView = (ViewGroup) findViewById(R.id.activity_progress_contact_info_group);
        mActivityTitle = (TextSwitcher) findViewById(R.id.activity_progress_title);
        mContactDisplayNameView = ((TextView) findViewById(R.id.activity_progress_name));
        mContactPhoneNumberView = ((TextView) findViewById(R.id.activity_progress_number));
        mContactEmailView = ((TextView) findViewById(R.id.activity_progress_email));
        mContactPhotoView = ((ImageView) findViewById(R.id.activity_progress_photo));
        mProgressBar = (ProgressBar) findViewById(R.id.activity_progress_progress_bar);
        mStopButton = (Button) findViewById(R.id.activity_progress_stop_service);
    }

    private void performInitialSetup() {
        mContactInfoView.setAlpha(0f);
        mProgressBar.setMax(mRequestedNumber);

        mHeader = getResources().getString(R.string.progress_header);
        mAnimationLength = getAnimationDuration();

        mActivityTitle.setInAnimation(this, android.R.anim.fade_in);
        mActivityTitle.setOutAnimation(this, android.R.anim.fade_out);
        mActivityTitle.setAnimateFirstView(false);

        mActivityTitle.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView holder = new TextView(ProgressActivity.this);
                holder.setTextSize(26);
                return holder;
            }
        });

        mActivityTitle.setText(getResources().getText(R.string.waiting));
        mStopButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(this, GeneratorService.class);
        bindService(serviceIntent, this, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = ((GeneratorServiceBinder) iBinder).getService();
        mService.setOnGenerateProgressListener(this);
        mService.setOnGenerateResultListener(this);
        mService.generate(mRequestedNumber, mFetchImages, mGender);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public void onGenerateProgress(@FloatRange(from = 0.0f, to = 1.0f) float progress, @IntRange(from = 0) int iStep,
            @IntRange(from = 0) int generated) {
        Person person = mService.getLastGeneratedPerson();
        if (person != null) {
            mActivityTitle.setText(mHeader);
            mContactInfoView.animate().setDuration(mAnimationLength).alpha(1f);
            mContactDisplayNameView.setText(person.getDisplayName());
            mContactPhoneNumberView.setText(person.getPhone());
            mContactEmailView.setText(person.getEmail());
            if (person.getImage() != null) {
                mContactPhotoView.setImageBitmap(person.getImage());
            } else {
                int drawable = AVATARS[(int) Math.round(Math.random() * AVATARS.length)];
                mContactPhotoView.setImageResource(drawable);
            }
        }

        mProgressBar.setProgress(iStep);
    }

    @Override
    public void onGenerateResult(@IntRange(from = 0) int requested, @IntRange(from = 0) int generated, boolean forced) {
        Log.d(TAG, "Requested: " + requested + ", generated: " + generated);
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_progress_stop_service:
                if (mService != null) {
                    mService.stopGenerating();
                }

                Intent serviceStopper = new Intent(ProgressActivity.this, GeneratorService.class);
                stopService(serviceStopper);

                Intent goToStartScreen = new Intent(ProgressActivity.this, MainActivity.class);
                startActivity(goToStartScreen);

                finish();
                break;

            default:
                Log.d(TAG, "Unprocessed click.");
        }
    }

    private long getAnimationDuration() {
        return getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }
}
