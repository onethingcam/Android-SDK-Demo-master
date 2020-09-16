# Android BVCU SDK
MCP和MPU所有的功能均基于BVCU SDK进行开发   
   
#### SDK内部封装了以下基础组件:
- [x] 音视频硬件编解码,支持H264、H265、G726、AAC
- [x] 网络数据协议，支持基于UDP和TCP收发RTP数据和SIP信令数据
- [x] 基于OpenGLes的视频渲染
- [x] 基于OpenSLes的音频播放
- [x] 回声消除AEC、降噪NS、语音检测VAD等音频算法  
- [x] 电子围栏和线路规划算法  

开发者可以用简单的十几行SDK调用快速开发出一个简单的类似MPU音视频发送和对讲功能的设备端APP或者类似MCU音视频接收预览功能的客户端APP。   
   
SDK Demo为Android Studio工程，包含一些基础功能API的调用。可以供开发者参考。   
SDK Lib包含在Demo libs目录下的bvcu.jar、gson.jar和jniLibs目录下动态库文件。

---  


### SDK主要功能接口如下：
- 音视频传输和对讲 
- MCU设备音视频预览 
- 远程文件检索、下载、上传（包括平台和设备） 
- 远程设备查询、配置 
- 电子围栏、线路规划
- 群组创建、删除、配置、 
- 群组的文字、文件、GPS和表情等IM消息收发   

 ### 商务合作
 -Skype: mike.liu820 
 -Tel/waht's app: +86 138 2350 1610
 -Wechat: mikelwl  





