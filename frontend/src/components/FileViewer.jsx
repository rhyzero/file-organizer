import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import LoadingSpinner from './LoadingSpinner';
import { Document, Page, pdfjs } from 'react-pdf';
import mammoth from 'mammoth';
import './FileViewer.css';
import 'react-pdf/dist/esm/Page/AnnotationLayer.css';
import 'react-pdf/dist/esm/Page/TextLayer.css';

pdfjs.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs';

const FileViewer = ({ isOpen, file, onClose }) => {
  const [fileContent, setFileContent] = useState(null);
  const [numPages, setNumPages] = useState(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [scale, setScale] = useState(1.5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { getIdToken } = useAuth();

  //Load file content when modal opens
  useEffect(() => {
    if (isOpen && file) {
      loadFileContent();
    } else {
      //Reset state when modal closes
      setFileContent(null);
      setError('');
      setNumPages(null);
      setPageNumber(1);
      setScale(1.5);
    }
  }, [isOpen, file]);

  const loadFileContent = async () => {
    setLoading(true);
    setError('');
    setFileContent(null);
    setPageNumber(1);

    try {
      const idToken = await getIdToken();
      if (!idToken) {
        throw new Error('Authentication token not available');
      }

      const response = await fetch(
        `http://localhost:5050/api/files/${file.fileId}/content`,
        {
          headers: {
            Authorization: `Bearer ${idToken}`,
          },
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch file content: ${errorText}`);
      }

      const fileType = file.mimeType;

      if (fileType === 'application/pdf') {
        //Handle PDF files
        const arrayBuffer = await response.arrayBuffer();
        setFileContent(arrayBuffer);
      } else if (
        fileType ===
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      ) {
        //Handle DOCX files
        const arrayBuffer = await response.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        setFileContent(result.value);
      } else if (fileType === 'application/msword') {
        setError(
          'DOC files are not supported for preview. Please open in Google Drive.'
        );
        return;
      } else {
        throw new Error(
          'File type not supported for preview. Only PDF and DOCX files can be previewed.'
        );
      }
    } catch (error) {
      console.error('Error loading file:', error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const onDocumentLoadSuccess = ({ numPages }) => {
    setNumPages(numPages);
    setError('');
  };

  const onDocumentLoadError = (error) => {
    console.error('PDF loading error:', error);
    setError('Failed to load PDF. The file might be corrupted or unsupported.');
  };

  const goToPrevPage = () => {
    setPageNumber((page) => Math.max(1, page - 1));
  };

  const goToNextPage = () => {
    setPageNumber((page) => Math.min(numPages, page + 1));
  };

  const zoomIn = () => {
    setScale((prevScale) => Math.min(prevScale + 0.25, 3.0));
  };

  const zoomOut = () => {
    setScale((prevScale) => Math.max(prevScale - 0.25, 0.5));
  };

  const resetZoom = () => {
    setScale(1.0);
  };

  const fitToWidth = () => {
    setScale(1.2);
  };

  if (!isOpen || !file) {
    return null;
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="file-viewer-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{file.fileName}</h3>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="file-viewer-content">
          {loading && (
            <div className="viewer-loading">
              <LoadingSpinner message="Loading file..." />
            </div>
          )}

          {error && (
            <div className="viewer-error">
              <p>Error: {error}</p>
              <button
                onClick={() => window.open(file.driveUrl, '_blank')}
                className="open-drive-btn"
              >
                Open in Google Drive
              </button>
            </div>
          )}

          {!loading && !error && fileContent && (
            <>
              {/* PDF Viewer with Text Selection */}
              {file.mimeType === 'application/pdf' && (
                <div className="pdf-viewer">
                  <div className="pdf-controls">
                    <div className="page-controls">
                      <button onClick={goToPrevPage} disabled={pageNumber <= 1}>
                        ← Previous
                      </button>
                      <span className="page-info">
                        Page {pageNumber} of {numPages || '?'}
                      </span>
                      <button
                        onClick={goToNextPage}
                        disabled={pageNumber >= numPages}
                      >
                        Next →
                      </button>
                    </div>

                    <div className="zoom-controls">
                      <button onClick={zoomOut} disabled={scale <= 0.5}>
                        🔍−
                      </button>
                      <span className="zoom-level">
                        {Math.round(scale * 100)}%
                      </span>
                      <button onClick={zoomIn} disabled={scale >= 3.0}>
                        🔍+
                      </button>
                      <button onClick={fitToWidth} className="fit-btn">
                        Fit
                      </button>
                      <button onClick={resetZoom} className="reset-btn">
                        100%
                      </button>
                    </div>
                  </div>

                  {/* PDF Document*/}
                  <div className="pdf-document-container">
                    <Document
                      file={fileContent}
                      onLoadSuccess={onDocumentLoadSuccess}
                      onLoadError={onDocumentLoadError}
                      loading={<LoadingSpinner message="Loading PDF..." />}
                      error={
                        <div className="pdf-error">
                          <p>Failed to load PDF</p>
                          <button
                            onClick={() => window.open(file.driveUrl, '_blank')}
                            className="open-drive-btn"
                          >
                            Open in Google Drive
                          </button>
                        </div>
                      }
                    >
                      <Page
                        pageNumber={pageNumber}
                        scale={scale}
                        renderTextLayer={true}
                        renderAnnotationLayer={false}
                        customTextRenderer={false}
                      />
                    </Document>
                  </div>
                </div>
              )}

              {/* DOCX Viewer */}
              {file.mimeType ===
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document' && (
                <div className="docx-viewer">
                  <div
                    dangerouslySetInnerHTML={{ __html: fileContent }}
                    className="docx-content"
                  />
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default FileViewer;
