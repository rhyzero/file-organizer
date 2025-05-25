import React from 'react';
import { DocumentIcon } from './icons/DocumentIcon';

const FileItem = ({ fileData, onRemove, showTags = false, tags = [] }) => {
  //Function to calculate file size to render
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="file-item">
      <div className="file-info">
        <DocumentIcon className="file-icon" />
        <div className="file-details">
          <h4>{fileData.name || fileData.fileName}</h4>
          <p>
            {fileData.size && formatFileSize(fileData.size)}
            {showTags && tags.length > 0 && (
              <>
                {fileData.size && ' • '}
                Tags: {tags.join(', ')}
                {fileData.documentType && ` • Type: ${fileData.documentType}`}
              </>
            )}
          </p>
        </div>
      </div>
      {onRemove && (
        <button className="remove-btn" onClick={onRemove}>
          Remove
        </button>
      )}
    </div>
  );
};

export default FileItem;
