本工程包含两部分 \
1 编码模块 \
   AudioRecord录音PCM音频数据，使用libmp3lame库编码MP3
   
2 解码模块 \
  Android的Audiotrack只能播放原始PCM音频，使用第三方库Libmad来对mp3文件解码称为PCM数据，再送给audiotrack播放即可。

 
编译环境和步骤 \
1 下载安装android-ndk-r17

2 cd jni/ \
  执行 ndk-build 生成so

NDK部分已经完成,APP部分还在继续开发中


