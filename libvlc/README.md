## 专家端请求方法 ##

#### 请求方法 ####

接口地址：http://server:port/admin/metadata
>接口的URL为 `voice/(?P[0-9]+)/source`返回的`server`和`port`

请求方法: GET

请求header：

参数 | 说明
-----|-----
Authorization | `voice/(?P[0-9]+)/source`返回的`auth`


参数 | 说明
-----|-----
mount | `voice/(?P[0-9]+)/source`返回的`point`
mode | updinfo(固定值)
song | 需要发送的指令

#### 返回值 ####

检查返回值中是否包含 `Metadata update successful`,存在则成功，否则失败。

**成功**

```xml
<?xml version="1.0"?>
<iceresponse>
    <message>Metadata update successful</message>
    <return>1</return>
</iceresponse>
```

**失败**

```html
// mount参数传递出错
<html>
    <head>
        <title>Error 400</title>
    </head>
    <body>
        <b>400 - Source does not exist</b>
    </body>
</html>

// mode参数传递错误
<?xml version="1.0"?>
<iceresponse>
    <message>No such action</message>
    <return>0</return>
</iceresponse>

// header错误
You need to authenticate
```

## 家长端libvlc如何使用 ##

### 清单文件添加如下内容###

```java
<!-- Internet -->
<uses-permission android:name="android.permission.INTERNET" />
<!-- normal -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### 使用lib ###


```java
// 引入包 
import com.dashu.open.audioplay.StreamPlayer;
```


```java
StreamPlayer player = new StreamPlayer(Context, handler);

// 开始播放网络流
player.start("http://101.200.77.249:8000/example123.mp3");

// 结束播放
player.stop();

Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case StreamPlayer.StreamPlayerPlaying:
				// 开始播放
				break;
			case StreamPlayer.StreamPlayerPaused:
				// 暂停播放
				break;
			case StreamPlayer.StreamPlayerStopped:
				// 停止播放
				break;
			case StreamPlayer.StreamPlayerTitle:
				// title变更(msg.obj String类型)
				break;
			case StreamPlayer.StreamPlayerPosition:
				// 播放位置变化(msg.obj Float类型)
				break;
			case StreamPlayer.StreamPlayerTime:
				// 播放时间变化(msg.obj Long类型)
				break;
			case StreamPlayer.StreamPlayerError:
				// 发生错误(msg.obj String类型)
				break;

			default:
				break;
			}
		};
	};
```