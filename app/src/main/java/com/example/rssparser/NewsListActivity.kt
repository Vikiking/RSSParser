package com.example.rssparser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject

class NewsListActivity : AppCompatActivity() {

    var mRequest: Disposable? = null
    lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_list_activity)

        mRecyclerView = findViewById(R.id.news_recycler_view)

        val observable =
            createReqeust("https://api.rss2json.com/v1/api.json?rss_url=http%3A%2F%2Flenta.ru%2Frss%2Flast24")
                .map { Gson().fromJson(it, FeedAPI::class.java) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        mRequest = observable.subscribe({
            val feed = Feed(
                it.items.mapTo(
                    RealmList<FeedItem>(),
                    { feed ->
                        FeedItem(
                            feed.title,
                            feed.link,
                            feed.enclosure.link,
                            feed.description
                        )
                    })
            )
            Realm.getDefaultInstance().executeTransaction { realm ->
                val oldList = realm.where(Feed::class.java).findAll()
                if (oldList.size > 0) {
                    for (item in oldList) {
                        item.deleteFromRealm()
                    }
                }
                realm.copyToRealm(feed)
            }
            showRecyclerView()
        }, {
            Log.e(LOG_TAG, "", it)
            showRecyclerView()
        })
    }

    private fun showRecyclerView() {
        Realm.getDefaultInstance().executeTransaction { realm ->
            val feed = realm.where(Feed::class.java).findAll()
            if (feed.size > 0) {
                mRecyclerView.adapter = RecyclerAdapter(feed[0]!!.items)
                mRecyclerView.layoutManager = LinearLayoutManager(this)
            }
        }
    }

    override fun onDestroy() {
        mRequest?.dispose()
        super.onDestroy()
    }

    inner class RecyclerAdapter(val items: RealmList<FeedItem>) :
        RecyclerView.Adapter<RecyclerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
            val inflater = LayoutInflater.from(parent!!.context)
            val view = inflater.inflate(
                R.layout.list_item,
                parent,
                false
            )

            return RecyclerHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {
            val item = items[position] as FeedItem
            holder.bind(item)
        }

    }

    inner class RecyclerHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: FeedItem) {
            val title = itemView.findViewById<TextView>(R.id.item_title)
            val description = itemView.findViewById<TextView>(R.id.item_description)
            val thumbnail = itemView.findViewById<ImageView>(R.id.item_thumbnail)
            title.text = item.title
            description.text = item.description

            Picasso.get().load(item.thumbnail)
                .into(thumbnail)

            itemView.setOnClickListener {
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.data = Uri.parse(item.link)
//                title.context.startActivity(intent)
                val context = title.context
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra(EXTRA_TITLE, item.title)
                intent.putExtra(EXTRA_THUMBNAIL, item.thumbnail)
                intent.putExtra(EXTRA_DESCRIPTION, item.description)
                context.startActivity(intent)
            }

        }
    }

    companion object {
        private const val LOG_TAG = "parser_log"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_THUMBNAIL = "extra_thumbnail"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

}

class FeedAPI(val items: ArrayList<FeedItemAPI>)

class FeedItemAPI(
    val title: String = "",
    val link: String = "",
    val thumbnail: String = "",
    val description: String = "",
    var enclosure: Enclosure = Enclosure()
) {
    class Enclosure {
        val link: String = ""
        val type: String = ""
        val length: String = ""
    }
}

open class Feed(var items: RealmList<FeedItem> = RealmList<FeedItem>()) : RealmObject()

open class FeedItem(
    var title: String = "",
    var link: String = "",
    var thumbnail: String = "",
    var description: String = ""
) : RealmObject()

//https://www.androidauthority.com/simple-rss-reader-full-tutorial-733245/

//http://lenta.ru/rss/last24
//https://api.rss2json.com/v1/api.json?rss_url=http%3A%2F%2Flenta.ru%2Frss%2Flast24

//{
//    "status": "ok"
//    "feed": {
//    "url": "http://lenta.ru/rss/last24"
//    "title": "Lenta.ru"
//    "link": "https://lenta.ru/"
//    "author": ""
//    "description": "Новости, статьи, фотографии, видео. Семь дней в неделю, 24 часа в сутки."
//    "image": "https://lenta.ru/images/small_logo.png"
//}
//    "items": [
//    {
//        "title": "Опубликованы данные разведки США о счетах россиян в американских банках"
//        "pubDate": "2020-09-21 09:45:00"
//        "link": "https://lenta.ru/news/2020/09/21/fincen/"
//        "guid": "https://lenta.ru/news/2020/09/21/fincen/"
//        "author": ""
//        "thumbnail": ""
//        "description": "Международный журналистский проект «Кассандра» опубликовал данные финансовой разведки США (FinCEN) о счетах россиян в американских банках и сомнительных операциях по ним. В проекте принимают участие журналисты 110 изданий из 88 стран. В расследовании упоминаются материалы из 2,1 тысячи отчетов банков. <br> "
//        "content": "Международный журналистский проект «Кассандра» опубликовал данные финансовой разведки США (FinCEN) о счетах россиян в американских банках и сомнительных операциях по ним. В проекте принимают участие журналисты 110 изданий из 88 стран. В расследовании упоминаются материалы из 2,1 тысячи отчетов банков. <br> "
//        "enclosure": {
//        "link": "https://icdn.lenta.ru/images/2020/09/21/12/20200921123158942/pic_a0daffd2b891b75f9ae00282ff383463.jpg"
//        "type": "image/jpeg"
//        "length": 98786
//    }
//        "categories": [
//        "Экономика"
//        ]
//    }
//    {