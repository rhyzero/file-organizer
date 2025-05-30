import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import FileBrowser from './FileBrowser';
import FileDropzone from './FileDropzone';
import FileList from './FileList';
import UploadResults from './UploadResult';
import './DocumentUploader.css';

const DocumentUploader = () => {
  //State variables
  //Variables for files being uploaded, uploading status, and results
  const [files, setFiles] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [results, setResults] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const { getIdToken } = useAuth();

  //Function to handle file uploads by giving them unique IDs
  //ID is randomly generated for reacts key attribute before being sent to backend
  const handleFilesAdded = (newFiles) => {
    const formattedFiles = newFiles.map((file) => ({
      file,
      id: Math.random().toString(36).substr(2, 9),
      name: file.name,
      size: file.size,
      type: file.type,
    }));

    setFiles((prev) => [...prev, ...formattedFiles]);
  };

  //Function to remove files using their ID
  const handleFileRemove = (id) => {
    setFiles((prev) => prev.filter((file) => file.id !== id));
  };

  const handleUpload = async () => {
    if (files.length === 0) return;

    setIsUploading(true);
    setResults(null);

    try {
      //Debug: Check if user is authenticated
      console.log(
        'Current user:',
        getIdToken ? 'getIdToken available' : 'getIdToken not available'
      );

      //Get Firebase ID token for authentication
      const idToken = await getIdToken();
      console.log(
        'ID Token received:',
        idToken ? 'Token obtained' : 'No token'
      );

      if (!idToken) {
        throw new Error(
          'Authentication token not available. Please try logging out and back in.'
        );
      }

      const uploadResults = [];

      //Files are uploaded one by one since backend only handles one file at a time
      for (const fileData of files) {
        console.log('Uploading file:', fileData.name);

        const formData = new FormData();
        formData.append('document', fileData.file);

        const response = await fetch('http://localhost:5050/upload', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${idToken}`, //Include FirebaseUID token in the request headers
          },
          body: formData,
        });

        console.log('Response status:', response.status);

        if (!response.ok) {
          const errorText = await response.text();
          console.error('Server response:', errorText);

          if (response.status === 401) {
            throw new Error('Authentication failed. Please log in again.');
          }

          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const responseData = await response.json();
        console.log('Backend response:', responseData);

        //Add the individual result to our results array
        uploadResults.push(responseData);
      }

      setResults(uploadResults);
      console.log('All uploads completed:', uploadResults);
      setFiles([]);

      //Trigger file browser refresh
      setRefreshTrigger((prev) => prev + 1);
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed: ' + error.message);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="document-uploader">
      <div className="header">
        <h1>Document Classifier</h1>
        <p>Upload your documents to automatically classify and tag them</p>
      </div>

      <div className="main-content">
        {/* Left Column - File Browser */}
        <div className="file-browser-column">
          <FileBrowser refreshTrigger={refreshTrigger} />
        </div>

        {/* Right Column - Upload Section */}
        <div className="upload-column">
          <div className="upload-section">
            <FileDropzone onFilesAdded={handleFilesAdded} />

            {files.length > 0 && (
              <FileList files={files} onFileRemove={handleFileRemove} />
            )}

            <button
              className="upload-btn"
              onClick={handleUpload}
              disabled={files.length === 0 || isUploading}
            >
              {isUploading
                ? 'Processing...'
                : `Upload & Classify ${files.length} File${
                    files.length !== 1 ? 's' : ''
                  }`}
            </button>
          </div>

          {files.length === 0 && (
            <UploadResults isUploading={isUploading} results={results} />
          )}
        </div>
      </div>
    </div>
  );
};

export default DocumentUploader;
