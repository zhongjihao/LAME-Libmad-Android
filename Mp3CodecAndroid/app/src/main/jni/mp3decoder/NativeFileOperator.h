/*************************************************************************
    > File Name: NativeFileOperator.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 17时07分51秒
 ************************************************************************/

#ifndef _NATIVE_FILE_OPERATOR_H
#define _NATIVE_FILE_OPERATOR_H

#include <unistd.h>  
#include <sys/stat.h>  
#include <sys/time.h>  
#include <sys/types.h>  
#include <stdlib.h>  
#include <fcntl.h>

#define _CREATE 0
#define _RDONLY 1
#define _WRONLY 2
#define _RDWR   3

#define _FMODE_READ     _RDONLY  
#define _FMODE_WRITE    _WRONLY  
#define _FMODE_CREATE   _CREATE  
#define _FMODE_RDWR     _RDWR 

int file_open(const char* filename,int access);
int file_read(int fd,unsigned char* buf,int size);
int file_write(int fd,unsigned char* buf,int size);
int64_t file_seek(int fd,int64_t pos,int whence);
int file_close(int fd);

#endif

