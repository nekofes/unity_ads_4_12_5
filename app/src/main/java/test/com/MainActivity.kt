package test.com

import android.graphics.Color
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import test.com.databinding.ActivityMainBinding

import androidx.core.content.getSystemService
//ViewCompat.setOnApplyWindowInsetsListenerのため使用しています
import androidx.core.view.ViewCompat
//BannerViewを利用可能にするため
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import com.unity3d.services.banners.BannerErrorInfo
//4.12.5 で存在する正しいインポートパッケージ
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds

import kotlinx.coroutines.*

//デリゲート（委譲）用のインターフェースを定義
interface OnScreenSizeCalculatedListener
{
    fun onSizeCalculated(screenWidth: Int, screenHeight: Int, statusBarHeight: Int, navigationBarHeight: Int)
}

class MainActivity : AppCompatActivity(), OnScreenSizeCalculatedListener
{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    //広告のインスタンスを記憶しておく変数（追加）
    private var unityBannerView: BannerView? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //OSのサイズ確定タイミングを捉えます。
        //サイズ確定を待つ処理です。
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->

            val screenSize = screenSize()
            val screenWidth = screenSize.first
            val screenHeight = screenSize.second

            val controlSize = screenUsingBar()
            val statusBarHeight = controlSize.first
            val navigationBarHeight = controlSize.second

            //デリゲートメソッドを呼び出して、実際の処理を委譲します。
            onSizeCalculated(screenWidth, screenHeight, statusBarHeight, navigationBarHeight)

            ViewCompat.setOnApplyWindowInsetsListener(view, null) //ループ防止
            windowInsets
        }
    }
    private fun screenSize() : Pair<Int, Int>
    {
        var width: Int = 0
        var height: Int = 0

        if (Build.VERSION.SDK_INT < 30) //API 29以下（Android 10以下）
        {
            //従来の取得方法
            width = this.resources.displayMetrics.widthPixels
            height = this.resources.displayMetrics.heightPixels
        }
        else //API 30以上（Android 11以上）
        {
            //近年のAndroidで推奨されている新しい統一パターン
            val bounds = windowManager.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        }

        return Pair(width, height)
    }
    /*
    画面の開始位置を計算するため
    ステータスバーやナビゲーションバーの高さを返す処理
    */
    private fun screenUsingBar() : Pair<Int, Int>
    {
        var statusBarHeight : Int = 0
        var navigationBarHeight : Int = 0
        if(Build.VERSION.SDK_INT >= 30)
        {
            statusBarHeight = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars()).top
            navigationBarHeight = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()).bottom
        }
        else
        {
            var resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            statusBarHeight =
                if (resourceId > 0)
                {
                    resources.getDimensionPixelSize(resourceId)
                }
                else
                {
                    0
                }
            resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            navigationBarHeight =
                if (resourceId > 0)
                {
                    resources.getDimensionPixelSize(resourceId)
                }
                else
                {
                    0
                }
        }

        return Pair(statusBarHeight, navigationBarHeight)
    }

    //デリゲートされた処理をここに集約します。
    override fun onSizeCalculated(screenWidth: Int, screenHeight: Int, statusBarHeight: Int, navigationBarHeight: Int)
    {
        val adHeight = adHeight()
        control(screenWidth, screenHeight, adHeight,statusBarHeight,navigationBarHeight)
        initUnityAdsWithRetry()
    }
    //バナー広告の高さ
    private fun adHeight(): Float
    {
        //ディスプレイ専用のコンテキスト（環境情報）を作成
        val displayManager = getSystemService<DisplayManager>()?.getDisplay(Display.DEFAULT_DISPLAY)
        //ディスプレイ専用のコンテキスト（環境情報）を作成
        val displayContext = createDisplayContext(displayManager!!)
        //画面の密度（倍率）を取得
        val density = displayContext.resources.displayMetrics.density
        //dpからpx（ピクセル）への変換と返却
        return 50 * density
    }
    private fun initUnityAdsWithRetry()
    {
        val result=adTestMethod()
        val gameId = result.first //game id
        val testMode = result.second //デバッグ中ならtrue

        UnityAds.initialize(this, gameId, testMode, object : IUnityAdsInitializationListener
        {
            override fun onInitializationComplete()
            {
                //初期化完了後にバナーを読み込む
                loadUnityBanner()
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String)
            {
                binding.adArea.visibility= View.VISIBLE
                Log.e("UnityAds", "初期化失敗: $message")

                //10秒後に自動で再初期化を試みます（コルーチン）。
                CoroutineScope(Dispatchers.Main).launch {
                    delay(10000) //10秒待機
                    //自身の関数を再帰呼び出ししてリトライ
                    initUnityAdsWithRetry()
                }
            }
        })
    }
    fun loadUnityBanner()
    {
        val placementId = placementId()
        val bannerView = BannerView(this, placementId, UnityBannerSize(320, 50))
        val layout = binding.adView

        //ロードの状態を監視するリスナー
        bannerView.listener = object : BannerView.IListener
        {
            override fun onBannerLoaded(bannerAdView: BannerView?)
            {
                Log.d("UnityAds", "★バナーのロードに成功しました。")
                binding.adArea.visibility= View.INVISIBLE
            }
            override fun onBannerShown(bannerAdView: BannerView?)
            {
                Log.d("UnityAds", "バナーが表示されました。")
                binding.adArea.visibility= View.INVISIBLE
            }
            override fun onBannerFailedToLoad(bannerAdView: BannerView?, error: BannerErrorInfo?)
            {
                Log.e("UnityAds", "❌バナーのロードに失敗しました。理由: ${error?.errorMessage}")
                binding.adArea.visibility= View.VISIBLE

                //【対策】広告が取れなくても、枠のサイズ（高さ144pxなど）を強制的に固定して残します。
                //これにより、No Fillになっても領域が 0,0-0,0 になって消えるのを防ぎます。
                val layoutParams = binding.adView.layoutParams
                layoutParams.height = adHeight().toInt() // プロジェクトのバナーの高さ（dpかpxか）に合わせて数値を調整してください。
                binding.adView.layoutParams = layoutParams

                // 確認用に、広告枠の背景色を黄色などにして「枠が消えていないこと」を目視できるようにします。
                //binding.adView.setBackgroundColor(android.graphics.Color.YELLOW)
                binding.adView.setBackgroundColor(Color.argb(100,255,255,100))
            }
            override fun onBannerClick(bannerAdView: BannerView?) {}
            override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
        }

        //画面にバナーを追加してロードを実行します。
        layout.removeAllViews()
        layout.addView(bannerView)
        bannerView.load()
    }

    private fun adTestMethod() : Pair<String, Boolean>
    {
        //testMethod()の戻り値
        //0:本番環境
        //1:テスト環境
        val result:Int=testMethod()
        //123456789:あなたのUnity Game ID
        val gameId = if(result==0)"123456789" else "4550882"
        val isTestMode = if(result==0)false else true // デバッグ中ならtrue（テストモードON）

        return Pair(gameId, isTestMode)
    }
    private fun placementId() : String
    {
        val result:Int = testMethod()
        //xxx:Unity登録画面で取得したIDを登録
        return if(result==0) "xxx" else "Banner_Android"
    }
    /*
    BuildConfig.DEBUGの実行結果と同等の処理
    デバッグ中ならtrue（テストモードON）
    testMethod()の戻り値
    0:本番環境
    1:テスト環境
    */
    private fun testMethod(): Int
    {
        // 1. 【バックアップ手段】Androidシステム全体のログ設定からデバッグ状態を推測します。
        // コマンド（adb shell setprop log.tag.MYSIGN DEBUG）等で開発者が制御できる裏ルートです。
        val isDebug = (android.os.Build.VERSION.SDK_INT >= 0) &&
                      (android.util.Log.isLoggable("MYSIGN", android.util.Log.DEBUG))

        // 2. 【メインの手段】クラスの存在チェック
        /*
        2. もしくは、より確実でエラーが絶対に出ない「クラスの存在チェック」を使います。
        あなたのアプリのパッケージ名にあわせてください。
        大元のパッケージ名が test.com の場合、自動生成されるクラスのフルネームは test.com.BuildConfig になります。
        */
        val hasDebug = try
        {
            Class.forName("test.com.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        }
        catch (e: Exception)
        {
            false
        }

        //メイン(hasDebug)がダメでも、バックアップ(isDebug)が true ならテスト環境(1)とみなします。
        return if (hasDebug || isDebug)
        {
            1
        }
        else
        {
            0
        }
    }
    //画面コントロール
    private fun control(screenWidth:Int, screenHeight:Int, adHeight: Float, statusBarHeight: Int, navigationBarHeight: Int)
    {
        var y = 0F

        y = (statusBarHeight + navigationBarHeight).toFloat()

        binding.adArea.x = 0F
        binding.adArea.y = y
        binding.adArea.layoutParams.width = screenWidth
        binding.adArea.layoutParams.height = adHeight.toInt()

        binding.adArea.typeface = Typeface.DEFAULT_BOLD
        binding.adArea.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        binding.adArea.setBackgroundColor(Color.argb(240,102,153,255))
        binding.adArea.setTextColor(Color.WHITE)
        binding.adArea.textSize=16F
        binding.adArea.text = "広告が表示されるスペース"

        //計算で位置決めしているため広告を表示するエリアの位置決め座標も同様に計算配置します。
        binding.adView.x = 0F
        binding.adView.y = y
        binding.adView.setBackgroundColor(Color.argb(100,245,245,245))
    }
    //画面が閉じるときに自動で呼ばれる後片付け（追加）
    override fun onDestroy()
    {
        //Unity Adsのバナーを完全に破棄して通信を止めます。
        unityBannerView?.destroy()
        unityBannerView = null // ゴミ箱を空にします。

        //最後に親（OS）の終了処理を呼びます（これは必須）。
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId)
        {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}