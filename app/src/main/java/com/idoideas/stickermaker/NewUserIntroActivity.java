package com.idoideas.stickermaker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.app.OnNavigationBlockedListener;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.idoideas.stickermaker.WhatsAppBasedCode.StickerPackListActivity;

public class NewUserIntroActivity extends IntroActivity {

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setButtonBackVisible(false);
        addSlide(new SimpleSlide.Builder()
                .title(R.string.welcome_to_stickerMaker)
                .description(R.string.the_perfect_solution)
                .image(R.drawable.stickermakerlogo)
                .background(R.color.colorAccent)
                .scrollable(false)
                .build());

/*
        if(!checkIfBatteryOptimizationIgnored() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.We_recognized)
                    .description(R.string.in_order_battery)
                    .background(R.color.colorAccent)
                    .scrollable(false)
                    .buttonCtaLabel(R.string.lets_go)
                    .buttonCtaClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivityForResult(intent, 4113);
                        }
                    })
                    .build());
        }
*/

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.we_need)
                    .description(R.string.in_order_storage)
                    .background(R.color.colorAccent)
                    .scrollable(false)
                    .buttonCtaLabel(R.string.grant_permissions)
                    .buttonCtaClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            verifyStoragePermissions(NewUserIntroActivity.this);
                        }
                    })
                    .build());
        }

        addSlide(new SimpleSlide.Builder()
                .title(R.string.everything_is_set)
                .description(getString(R.string.you_can_start) + "\n"+
                        getString(R.string.please_notice) +
                        getString(R.string.uninstalling_the_app) + " "+
                        getString(R.string.and_will_be_lost))
                .background(R.color.colorAccent)
                .scrollable(false)
                .build());

        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if(i>0 && i!=getSlides().size()-1){
                    setNavigation(false, true);
                } else {
                    setNavigation(true, false);
                }

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 4113){
            if(checkIfBatteryOptimizationIgnored()){
                setNavigation(true, false);
                nextSlide();
            }
        }
    }

    private boolean checkIfBatteryOptimizationIgnored(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        } else {
            return true;
        }
    }


    private void setNavigation(boolean forward, boolean backward){
        setNavigationPolicy(new NavigationPolicy() {
            @Override
            public boolean canGoForward(int i) {
                return forward;
            }

            @Override
            public boolean canGoBackward(int i) {
                return backward;
            }
        });
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    1
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setPositiveButton(R.string.lets_go, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    verifyStoragePermissions(NewUserIntroActivity.this);
                                }
                            })
                            .create();
                    alertDialog.setTitle(getString(R.string.notice));
                    alertDialog.setMessage(getString(R.string.allowing_storage_permissions));
                    alertDialog.show();
                } else {
                    setNavigation(true, false);
                    nextSlide();
                }
                break;
        }
    }
}
