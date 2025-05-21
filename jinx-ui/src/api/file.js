import request from '@/utils/request';

/**
 * 上传文件
 * @param {Object} file - 文件对象
 * @param {String} fileKey - 文件唯一标识，支持目录形式：exampledir/exampleobject.txt
 * @returns {Promise} - 返回Promise对象
 */
export function uploadFile(file, fileKey) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('fileKey', fileKey);
  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
