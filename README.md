本工程包含三部分 \
1 编码模块 \
   AudioRecord录音PCM音频数据，使用libmp3lame库编码MP3
   
2 解码模块 \
  Android的Audiotrack只能播放原始PCM音频，使用第三方库Libmad来对mp3文件解码称为PCM数据，再送给audiotrack播放即可。
  
3 网络传输模块 \
 将本地客户端录的音频文件发送给服务端

 
编译环境和步骤 \
  下载安装android-ndk-r9d ,高版本NDK-r16编译失败，具体原因待查

cd jni/ \
执行 ndk-build 生成so


