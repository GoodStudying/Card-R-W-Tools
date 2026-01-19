package cn.ratnoumi.bcardtools

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.ratnoumi.bcardtools.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.okBtn.setOnClickListener {
            val sp = getSharedPreferences("app_config", Context.MODE_PRIVATE)
            sp.edit().putBoolean("about_skip", true).apply()
            finish()
        }
    }

}