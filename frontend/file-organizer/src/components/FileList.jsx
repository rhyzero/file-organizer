import React from 'react';
import FileItem from './FileItem';

const FileList = ({ files, onFileRemove }) => {
  return (
    <div className="file-list">
      {files.map((fileData) => (
        <FileItem
          key={fileData.id}
          fileData={fileData}
          onRemove={() => onFileRemove(fileData.id)}
        />
      ))}
    </div>
  );
};

export default FileList;
