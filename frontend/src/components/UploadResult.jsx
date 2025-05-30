import React from 'react';
import FileItem from './FileItem';
import LoadingSpinner from './LoadingSpinner';
import { CheckCircleIcon } from './icons/CheckCircleIcon';

const UploadResults = ({ isUploading, results }) => {
  if (isUploading) {
    return (
      <div className="results-section">
        <LoadingSpinner message="Classifying your documents..." />
      </div>
    );
  }

  if (!results) {
    return null;
  }

  //Safety check - ensure results is an array
  if (!Array.isArray(results)) {
    console.error('Results is not an array:', results);
    return (
      <div className="results-section">
        <h3>Classification Results</h3>
        <div className="error-message">
          <p>Error: Invalid response format received from server.</p>
          <pre>{JSON.stringify(results, null, 2)}</pre>
        </div>
      </div>
    );
  }

  return (
    <div className="results-section">
      <h3>Classification Results</h3>
      <div className="results-scroll-wrapper">
        {results.map((result, index) => (
          <div key={index} className="file-item">
            <div className="file-info">
              <CheckCircleIcon className="file-icon success" />
              <div className="file-details">
                <h4>{result.fileName}</h4>
                <p>
                  Tags:{' '}
                  {result.tags && result.tags.length > 0
                    ? result.tags.join(', ')
                    : 'No tags'}{' '}
                  • Type: {result.documentType || 'Unknown'}
                  {result.confidence &&
                    ` • Confidence: ${(result.confidence * 100).toFixed(0)}%`}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UploadResults;
