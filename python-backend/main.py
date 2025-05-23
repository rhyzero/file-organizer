from flask import Flask, request, jsonify
import logging
import time
import torch
import numpy as np
from transformers import pipeline

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Check if CUDA GPU is available
if torch.cuda.is_available():
    # Use first GPU with CUDA
    DEVICE = 0
    logger.info(f"Using GPU: {torch.cuda.get_device_name(DEVICE)}")
else:
    # Use CPU, will be slower
    DEVICE = -1
    

# Define document tags - 20 total tags
DOCUMENT_CATEGORIES = [
    "legal", "financial", "technical", "marketing", "hr", 
    "strategic", "research", "policy", "report", "product",
    "customer", "correspondence", "administrative", "compliance",
    "math-science", "humanities", "computer", "business-studies", 
    "arts", "assignment"
]

# Split into professional vs academic for more specification
TAG_HIERARCHY = {
    "professional": [
        "legal", "financial", "technical", "marketing", "hr", 
        "strategic", "research", "policy", "report", "product",
        "customer", "correspondence", "administrative", "compliance"
    ],
    "academic": [
        "math-science", "humanities", "computer", "business-studies", 
        "arts", "assignment"
    ]
}

# Load model
classifier = pipeline("zero-shot-classification", 
                    model="facebook/bart-large-mnli",
                    device=DEVICE)

# Document type specific words
DOCUMENT_RULES = {
    # Documents that often contain these keywords are likely HR documents
    "hr": ["employment", "offer letter", "job offer", "salary", "compensation", 
           "benefits", "vacation", "pto", "paid time off", "401k", "health insurance",
           "onboarding", "employee", "personnel"],
           
    # Financial document indicators
    "financial": ["invoice", "receipt", "payment", "statement", "balance sheet", 
                 "income statement", "profit", "loss", "revenue", "expense", 
                 "budget", "forecast", "quarterly report", "annual report"],
                 
    # Legal document indicators
    "legal": ["agreement", "contract", "terms", "conditions", "confidentiality", 
             "non-disclosure", "intellectual property", "liability", "warranty",
             "indemnification", "governing law", "jurisdiction", "hereby"],
             
    # Technical document indicators
    "technical": ["specification", "requirements", "architecture", "design", 
                 "implementation", "database", "algorithm", "protocol", 
                 "interface", "api", "documentation", "code", "software"],
}

# Endpoint for classification
@app.route('/classify', methods=['POST'])
def classify_document():
    start_time = time.time()
    
    # Stores JSON data from the POST request into data variable
    data = request.json
    
    # Check if there is a text attribute in the JSON data
    if not data or 'text' not in data:
        return jsonify({"error": "No text provided"}), 400
    
    text = data['text']
    
    # Limit text length to avoid memory issues and improve speed
    max_tokens = 1024
    # Truncates text to only accept words equal to the number of tokens or less
    text = ' '.join(text.split()[:max_tokens])
    
    logger.info(f"Processing document with {len(text.split())} words")
    
    try:
        # First, check for keyword-based rules to improve accuracy
        # Stores tags that match between the text and from out predefined lists
        rule_based_tags = []
        text_lower = text.lower()
        
        # Loop to check for matching keywords from the predefined lists that appear in the text and add them
        # to the list
        for tag, keywords in DOCUMENT_RULES.items():
            for keyword in keywords:
                if keyword.lower() in text_lower:
                    if tag not in rule_based_tags:
                        rule_based_tags.append(tag)
                    break
        
        logger.info(f"Rule-based tags identified: {rule_based_tags}")
        
        # Run classification
        classification_start = time.time()
        result = classifier(text, DOCUMENT_CATEGORIES, multi_label=True)
        classification_time = time.time() - classification_start
        logger.info(f"Classification completed in {classification_time:.2f} seconds")
        
        # Create score dictionary
        # ex: {"hr" : 0.85, ...}
        scores = {label: float(score) for label, score in zip(result['labels'], result['scores'])}
        
        # Boost scores based on the found keywords in the text
        for tag in rule_based_tags:
            if tag in scores:
                scores[tag] = min(0.99, scores[tag] * 1.5)  # Boost but cap at 0.99
        
        # Get top scoring tags only
        # and limiting to maximum 3 tags total
        sorted_tags = sorted([(tag, score) for tag, score in scores.items()], 
                             key=lambda x: x[1], reverse=True)
        
        # Get primary tag (highest confidence)
        primary_tags = [sorted_tags[0][0]] if sorted_tags else []
        
        # Get up to 2 additional tags with score > 0.6
        secondary_tags = [tag for tag, score in sorted_tags[1:3] 
                         if score > 0.6 and tag not in primary_tags]
        
        # Determine document type
        professional_tags = [tag for tag in primary_tags + secondary_tags 
                            if tag in TAG_HIERARCHY["professional"]]
        academic_tags = [tag for tag in primary_tags + secondary_tags 
                        if tag in TAG_HIERARCHY["academic"]]
        
        document_type = "academic" if len(academic_tags) > 0 else "professional"
        
        logger.info(f"Document classified with primary tags: {primary_tags}")
        logger.info(f"Secondary tags: {secondary_tags}")
        logger.info(f"Document type: {document_type}")
        
        total_time = time.time() - start_time
        
        return jsonify({
            "primary_tags": primary_tags,
            "secondary_tags": secondary_tags,
            "document_type": document_type,
            "scores": scores,
            "rule_based_tags": rule_based_tags,
            "processing_time": total_time
        })
    
    except Exception as e:
        logger.error(f"Classification error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/status', methods=['GET'])
def get_status():
    status = {
        "status": "running",
        "using_gpu": torch.cuda.is_available(),
        "device": "GPU" if torch.cuda.is_available() else "CPU",
    }
    
    if torch.cuda.is_available():
        status["gpu_info"] = {
            "name": torch.cuda.get_device_name(0),
            "memory_allocated": f"{torch.cuda.memory_allocated(0)/1024**2:.2f} MB",
            "memory_reserved": f"{torch.cuda.memory_reserved(0)/1024**2:.2f} MB"
        }
    
    return jsonify(status)

@app.route('/supported-tags', methods=['GET'])
def get_supported_tags():
    """Return all supported tags and their hierarchy"""
    return jsonify({
        "all_tags": DOCUMENT_CATEGORIES,
        "tag_hierarchy": TAG_HIERARCHY,
        "document_rules": DOCUMENT_RULES
    })

@app.route('/classify-example', methods=['GET'])
def classify_example():
    example_text = """
    Employment Offer Letter
    Date: May 21, 2025
    Dear Jordan Lee,
    We are pleased to offer you employment with Horizon Analytics Corp. in the position of Data Analyst,
    reporting to the Director of Data Science. Your anticipated start date is July 1, 2025.
    This letter outlines the terms and conditions of your employment offer.
    1. Compensation. You will receive an annualized base salary of $85,000, paid in accordance with
    the Company's standard payroll schedule and subject to all required withholdings.
    """
    
    # Run the classifier on this example
    try:
        result = classifier(example_text, DOCUMENT_CATEGORIES, multi_label=True)
        
        # Apply rule-based tags
        rule_based_tags = []
        example_lower = example_text.lower()
        
        for tag, keywords in DOCUMENT_RULES.items():
            for keyword in keywords:
                if keyword.lower() in example_lower:
                    if tag not in rule_based_tags:
                        rule_based_tags.append(tag)
                    break
        
        # Create scores dictionary
        scores = {label: float(score) for label, score in zip(result['labels'], result['scores'])}
        
        # Get the top scoring tag
        sorted_tags = sorted([(tag, score) for tag, score in scores.items()], 
                           key=lambda x: x[1], reverse=True)
        
        primary_tags = [sorted_tags[0][0]] if sorted_tags else []
        
        return jsonify({
            "example_text": example_text[:200] + "...",
            "primary_tags": primary_tags,
            "rule_based_tags": rule_based_tags,
            "all_scores": scores
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    try:
        sample_text = "This is a sample document to warm up the classification model."
        _ = classifier(sample_text, ["test"], multi_label=False)
    except Exception as e:
        logger.error(f"Error during warm-up: {str(e)}")
    
    # Start the Flask app
    logger.info("Starting Flask server on port 5000...")
    app.run(host='0.0.0.0', port=5000)