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
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [fileToDelete, setFileToDelete] = useState(null);
  const [showRenameModal, setShowRenameModal] = useState(false);
  const [fileToRename, setFileToRename] = useState(null);
  const [newFileName, setNewFileName] = useState('');

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

  //Show delete confirmation modal
  const showDeleteConfirmation = (file) => {
    setFileToDelete(file);
    setShowDeleteModal(true);
  };

  //Actual delete function (called after confirmation)
  const confirmDeleteFile = async () => {
    if (!fileToDelete) return;

    try {
      const idToken = await getIdToken();
      if (!idToken) {
        throw new Error('Authentication token not available');
      }

      const response = await fetch(
        `http://localhost:5050/api/files/${fileToDelete.fileId}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${idToken}`,
            'Content-Type': 'application/json',
          },
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to delete file: ${errorText}`);
      }

      const result = await response.json();

      if (typeof result === 'string') {
        throw new Error(result);
      }

      if (result.success) {
        //Remove file from local state
        setFiles((prevFiles) =>
          prevFiles.filter((f) => f.fileId !== fileToDelete.fileId)
        );
        console.log('File deleted successfully:', fileToDelete.fileName);

        //Close modal
        setShowDeleteModal(false);
        setFileToDelete(null);
      } else {
        throw new Error(result.message || 'Delete failed');
      }
    } catch (error) {
      console.error('Error deleting file:', error);
      alert('Failed to delete file: ' + error.message);
      //Close modal even on error
      setShowDeleteModal(false);
      setFileToDelete(null);
    }
  };

  //Cancel delete
  const cancelDelete = () => {
    setShowDeleteModal(false);
    setFileToDelete(null);
  };

  //Show rename modal
  const showRenameDialog = (file) => {
    setFileToRename(file);
    setNewFileName(file.fileName);
    setShowRenameModal(true);
  };

  //Actual rename function (called after confirmation)
  const confirmRenameFile = async () => {
    if (!fileToRename || !newFileName.trim()) return;

    //Don't proceed if name hasn't changed
    if (newFileName.trim() === fileToRename.fileName) {
      cancelRename();
      return;
    }

    try {
      const idToken = await getIdToken();
      if (!idToken) {
        throw new Error('Authentication token not available');
      }

      const response = await fetch(
        `http://localhost:5050/api/files/${fileToRename.fileId}/rename`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${idToken}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            fileName: newFileName.trim(),
          }),
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to rename file: ${errorText}`);
      }

      const result = await response.json();

      if (typeof result === 'string') {
        throw new Error(result);
      }

      if (result.success) {
        setFiles((prevFiles) =>
          prevFiles.map((f) =>
            f.fileId === fileToRename.fileId
              ? { ...f, fileName: newFileName.trim() } // Use local variable
              : f
          )
        );
        console.log(
          'File renamed successfully:',
          fileToRename.firstName,
          '->',
          newFileName.trim()
        );
        cancelRename();
      } else {
        throw new Error(result.message || 'Rename failed');
      }
    } catch (error) {
      console.error('Error renaming file:', error);
      alert('Failed to rename file: ' + error.message);
      //Close modal even on error
      cancelRename();
    }
  };

  //Cancel rename
  const cancelRename = () => {
    setShowRenameModal(false);
    setFileToRename(null);
    setNewFileName('');
  };

  //Handle Enter key in rename input
  const handleRenameKeyPress = (e) => {
    if (e.key === 'Enter') {
      confirmRenameFile();
    } else if (e.key === 'Escape') {
      cancelRename();
    }
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
          <div
            className="context-menu-item rename"
            onClick={() => {
              showRenameDialog(contextMenu.file);
              setContextMenu({ visible: false, x: 0, y: 0, file: null });
            }}
          >
            Rename
          </div>
          <div
            className="context-menu-item delete"
            onClick={() => {
              showDeleteConfirmation(contextMenu.file);
              setContextMenu({ visible: false, x: 0, y: 0, file: null });
            }}
          >
            Delete
          </div>
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

      {/* Delete Confirmation Modal */}
      {showDeleteModal && fileToDelete && (
        <div className="modal-overlay" onClick={cancelDelete}>
          <div className="delete-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Delete</h3>
            </div>
            <div className="modal-content">
              <div className="delete-icon">
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <polyline points="3,6 5,6 21,6"></polyline>
                  <path d="m19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"></path>
                  <line x1="10" y1="11" x2="10" y2="17"></line>
                  <line x1="14" y1="11" x2="14" y2="17"></line>
                </svg>
              </div>
              <p className="delete-message">
                Are you sure you want to delete{' '}
                <strong>"{fileToDelete.fileName}"</strong>?
              </p>
              <p className="delete-warning">
                This action cannot be undone. The file will be permanently
                removed from your Google Drive.
              </p>
            </div>
            <div className="modal-actions">
              <button className="cancel-btn" onClick={cancelDelete}>
                Cancel
              </button>
              <button
                className="delete-confirm-btn"
                onClick={confirmDeleteFile}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Rename Modal */}
      {showRenameModal && fileToRename && (
        <div className="modal-overlay" onClick={cancelRename}>
          <div className="rename-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Rename File</h3>
            </div>
            <div className="modal-content">
              <div className="rename-icon">
                <svg
                  width="32"
                  height="32"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path>
                </svg>
              </div>
              <p className="rename-message">Enter a new name for this file:</p>
              <input
                type="text"
                value={newFileName}
                onChange={(e) => setNewFileName(e.target.value)}
                onKeyDown={handleRenameKeyPress}
                className="rename-input"
                placeholder="Enter filename"
                autoFocus
              />
              <p className="rename-hint">
                Press Enter to confirm or Escape to cancel
              </p>
            </div>
            <div className="modal-actions">
              <button className="cancel-btn" onClick={cancelRename}>
                Cancel
              </button>
              <button
                className="rename-confirm-btn"
                onClick={confirmRenameFile}
                disabled={
                  !newFileName.trim() ||
                  newFileName.trim() === fileToRename.fileName
                }
              >
                Rename
              </button>
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
