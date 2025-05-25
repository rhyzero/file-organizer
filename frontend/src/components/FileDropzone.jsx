import React, { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { UploadIcon } from './icons/UploadIcon';

const FileDropzone = ({ onFilesAdded }) => {
  const onDrop = useCallback(
    (acceptedFiles, rejectedFiles) => {
      //Handle accepted files
      if (acceptedFiles.length > 0) {
        //Accepted files are sent to parent
        onFilesAdded(acceptedFiles);
      }

      //Handle rejected files
      if (rejectedFiles.length > 0) {
        alert(
          'Some files were rejected. Only PDF, DOC, and DOCX files are allowed.'
        );
      }
    },
    [onFilesAdded]
  );

  const {
    getRootProps,
    getInputProps,
    isDragActive,
    isDragAccept,
    isDragReject,
  } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
        ['.docx'],
    },
    multiple: true,
  });

  const getDropzoneClassName = () => {
    let className = 'dropzone';
    if (isDragActive) className += ' active';
    if (isDragAccept) className += ' accept';
    if (isDragReject) className += ' reject';
    return className;
  };

  return (
    <div {...getRootProps({ className: getDropzoneClassName() })}>
      <input {...getInputProps()} />
      <div className="dropzone-content">
        <UploadIcon className="upload-icon" />
        <div className="upload-text">
          {isDragActive ? 'Drop files here...' : 'Drag & drop files here'}
        </div>
        <div className="upload-hint">
          or click to browse • PDF, DOC, DOCX, supported
        </div>
      </div>
    </div>
  );
};

export default FileDropzone;
