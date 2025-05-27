import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import LoadingSpinner from './LoadingSpinner';
import { DocumentIcon } from './icons/DocumentIcon';
import './FileBrowser.css';

const FileBrowser = ({ refreshTrigger }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [contextMenu, setContextMenu] = useState({
    visible: false,
    x: 0,
    y: 0,
    file: null,
  });
  const [showProperties, setShowProperties] = useState(null);
  const [hoveredFile, setHoveredFile] = useState(null);

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

  //Handle right-click context menu
  const handleRightClick = (e, file) => {
    e.preventDefault();
    setContextMenu({
      visible: true,
      x: e.clientX,
      y: e.clientY,
      file: file,
    });
  };

  //Close context menu when clicking elsewhere
  useEffect(() => {
    const handleClick = () =>
      setContextMenu({ visible: false, x: 0, y: 0, file: null });
    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, []);

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
    if (!bytes) return 'Unknown size';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getFileIcon = (mimeType) => {
    return <DocumentIcon className="file-grid-icon" />;
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

      {/* Files Grid */}
      {!error && filteredFiles.length > 0 && (
        <div className="files-grid">
          {filteredFiles.map((file, index) => (
            <div
              key={file.fileId || index}
              className="file-grid-item"
              onContextMenu={(e) => handleRightClick(e, file)}
              onMouseEnter={() => setHoveredFile(file)}
              onMouseLeave={() => setHoveredFile(null)}
              onDoubleClick={() => window.open(file.driveUrl, '_blank')}
            >
              {getFileIcon(file.mimeType)}
              <span className="file-grid-name">{file.fileName}</span>

              {/* Hover tooltip showing tags */}
              {hoveredFile === file && file.tags && file.tags.length > 0 && (
                <div className="file-tooltip">
                  <div className="tooltip-tags">
                    {file.tags.map((tag, tagIndex) => (
                      <span key={tagIndex} className="tooltip-tag">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Context Menu */}
      {contextMenu.visible && (
        <div
          className="context-menu"
          style={{
            position: 'fixed',
            left: contextMenu.x,
            top: contextMenu.y,
            zIndex: 1000,
          }}
        >
          <div
            className="context-menu-item"
            onClick={() => {
              setShowProperties(contextMenu.file);
              setContextMenu({ visible: false, x: 0, y: 0, file: null });
            }}
          >
            Properties
          </div>
          <div
            className="context-menu-item"
            onClick={() => {
              window.open(contextMenu.file.driveUrl, '_blank');
              setContextMenu({ visible: false, x: 0, y: 0, file: null });
            }}
          >
            View File
          </div>
          <div className="context-menu-item rename">Rename</div>
          <div className="context-menu-item delete">Delete</div>
        </div>
      )}

      {/* Properties Modal */}
      {showProperties && (
        <div className="modal-overlay" onClick={() => setShowProperties(null)}>
          <div
            className="properties-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <h3>File Properties</h3>
              <button
                className="close-btn"
                onClick={() => setShowProperties(null)}
              >
                ×
              </button>
            </div>
            <div className="modal-content">
              <div className="property-row">
                <strong>Name:</strong> {showProperties.fileName}
              </div>
              <div className="property-row">
                <strong>Size:</strong> {formatFileSize(showProperties.size)}
              </div>
              <div className="property-row">
                <strong>Date:</strong> {formatDate(showProperties.uploadDate)}
              </div>
              <div className="property-row">
                <strong>Type:</strong> {showProperties.mimeType}
              </div>
              {showProperties.tags && showProperties.tags.length > 0 && (
                <div className="property-row tags-row">
                  <strong>Tags:</strong>
                  <div className="property-tags">
                    {showProperties.tags.map((tag, index) => (
                      <span key={index} className="property-tag">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* No Results */}
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
