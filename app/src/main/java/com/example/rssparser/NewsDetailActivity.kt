package com.example.rssparser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.news_detail_activity.*

class NewsDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_detail_activity)

        val title = intent.getStringExtra(NewsListActivity.EXTRA_TITLE)
        val thumbnail = intent.getStringExtra(NewsListActivity.EXTRA_THUMBNAIL)
        val description = intent.getStringExtra(NewsListActivity.EXTRA_DESCRIPTION)

        detail_title.text = title
        detail_description.text = description
        Picasso.get().load(thumbnail)
            .into(detail_image)

    }
}
