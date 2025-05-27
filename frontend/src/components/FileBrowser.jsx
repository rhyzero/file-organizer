import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import FileItem from './FileItem';
import LoadingSpinner from './LoadingSpinner';
import { DocumentIcon } from './icons/DocumentIcon';
import './FileBrowser.css';

const FileBrowser = ({ refreshTrigger }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const { getIdToken } = useAuth();

  //Fetch user's files from backend
  const fetchFiles = async () => {
    try {
      setLoading(true);
      setError('');

      const idToken = await getIdToken();
      if (!idToken) {
        throw new Error('Authentication token not available');
      }

      const response = await fetch('http://localhost:5050/api/files', {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${idToken}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch files: ${response.status}`);
      }

      const data = await response.json();

      //Handle case where backend returns error string instead of array
      if (typeof data === 'string') {
        throw new Error(data);
      }

      setFiles(Array.isArray(data) ? data : []);
      console.log('Fetched files:', data);
    } catch (error) {
      console.error('Error fetching files:', error);
      setError(error.message);
      setFiles([]);
    } finally {
      setLoading(false);
    }
  };

  //Initial load
  useEffect(() => {
    fetchFiles();
  }, []);

  //Refresh when upload completes
  useEffect(() => {
    if (refreshTrigger > 0) {
      fetchFiles();
    }
  }, [refreshTrigger]);

  //Filter files based on search term
  const filteredFiles = files.filter((file) => {
    if (!searchTerm) return true;

    const searchLower = searchTerm.toLowerCase();
    const fileName = file.fileName ? file.fileName.toLowerCase() : '';
    const tags = file.tags ? file.tags.join(' ').toLowerCase() : '';

    return fileName.includes(searchLower) || tags.includes(searchLower);
  });

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    try {
      return new Date(dateString).toLocaleDateString();
    } catch {
      return 'Unknown';
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <div className="file-browser">
        <div className="file-browser-header">
          <h3>Your Documents</h3>
        </div>
        <LoadingSpinner message="Loading your files..." />
      </div>
    );
  }

  return (
    <div className="file-browser">
      <div className="file-browser-header">
        <h3>Your Documents</h3>
        <div className="file-count">
          {files.length} file{files.length !== 1 ? 's' : ''}
        </div>
      </div>

      {/* Search Bar */}
      <div className="search-section">
        <input
          type="text"
          placeholder="Search files by name or tags..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
      </div>

      {/* Error State */}
      {error && (
        <div className="error-section">
          <p>Error loading files: {error}</p>
          <button onClick={fetchFiles} className="retry-btn">
            Try Again
          </button>
        </div>
      )}

      {/* Empty State */}
      {!error && files.length === 0 && (
        <div className="empty-state">
          <DocumentIcon className="empty-icon" />
          <h4>No documents yet</h4>
          <p>Upload your first document to get started!</p>
        </div>
      )}

      {/* Files List */}
      {!error && filteredFiles.length > 0 && (
        <div className="files-list">
          {filteredFiles.map((file, index) => (
            <div key={file.fileId || index} className="file-browser-item">
              <div className="file-info">
                <DocumentIcon className="file-icon" />
                <div className="file-details">
                  <h4 className="file-name">{file.fileName}</h4>
                  <div className="file-meta">
                    <span className="file-date">
                      {formatDate(file.uploadDate)}
                    </span>
                    {file.size && (
                      <>
                        <span className="separator">•</span>
                        <span className="file-size">
                          {formatFileSize(file.size)}
                        </span>
                      </>
                    )}
                  </div>
                  {file.tags && file.tags.length > 0 && (
                    <div className="file-tags">
                      {file.tags.map((tag, tagIndex) => (
                        <span key={tagIndex} className="tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
              <div className="file-actions">
                <a
                  href={file.driveUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="view-btn"
                >
                  View
                </a>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Filtered Results */}
      {!error &&
        searchTerm &&
        filteredFiles.length === 0 &&
        files.length > 0 && (
          <div className="no-results">
            <p>No files match "{searchTerm}"</p>
            <button
              onClick={() => setSearchTerm('')}
              className="clear-search-btn"
            >
              Clear Search
            </button>
          </div>
        )}
    </div>
  );
};

export default FileBrowser;
