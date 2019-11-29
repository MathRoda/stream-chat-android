package io.getstream.chat.example;

import android.os.Bundle;

import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager2.widget.ViewPager2;

import io.getstream.chat.example.adapter.ViewPagerAdapter;
import io.getstream.chat.example.databinding.ActivityMainBinding;

/**
 * This activity shows a list of channels
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    MenuItem prevMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.viewPager.setAdapter(createCardAdapter());
        binding.tabs.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_channel:
                    binding.viewPager.setCurrentItem(0);
                    break;
                case R.id.action_profile:
                    binding.viewPager.setCurrentItem(1);
                    break;
            }

            return false;
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (prevMenuItem != null)
                    prevMenuItem.setChecked(false);
                else
                    binding.tabs.getMenu().getItem(0).setChecked(false);

                binding.tabs.getMenu().getItem(position).setChecked(true);
                prevMenuItem = binding.tabs.getMenu().getItem(position);
            }
        });

        initToolbar(binding);
    }

    private ViewPagerAdapter createCardAdapter() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        return adapter;
    }

    private void initToolbar(ActivityMainBinding binding) {
        binding.toolbar.setTitle("Stream Chat");
        binding.toolbar.setSubtitle("sdk:" + BuildConfig.SDK_VERSION + " / " + BuildConfig.VERSION_NAME + " / " + BuildConfig.APPLICATION_ID);
    }
}