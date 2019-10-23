/*************************************************************************
    > File Name: NativeFileOperator.c
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 15时32分34秒
 ************************************************************************/

#include "NativeFileOperator.h"

int file_open(const char *filename, int access)
{
	int fd;
	if(access == _CREATE)
	{
		fd = open(filename,O_RDWR|O_CREAT|O_EXCL,0666);
	}
	else if(access == _WRONLY)
	{
		fd = open(filename,O_WRONLY|O_TRUNC);
	}
	else if(access == _RDONLY)
	{
		fd = open(filename,O_RDONLY);
	}
    else if(access == _RDWR)
	{
		fd = open(filename,O_RDWR);
	}
	else
	{
		return -2;
	}

	return fd;
}

int file_read(int fd, unsigned char *buf, int size)  
{
	return read(fd, buf, size);  
}  

int file_write(int fd, unsigned char *buf, int size)  
{
	return write(fd, buf, size);  
}  

int64_t file_seek(int fd, int64_t pos, int whence)  
{
    return lseek(fd, pos, whence);    
}

int file_close(int fd)  
{
	return close(fd);  
}

