from flask import Flask, request, jsonify, render_template_string
import tensorflow as tf
import pickle
import re
from tensorflow.keras.preprocessing.sequence import pad_sequences
import os

app = Flask(__name__)

model = None
tokenizer = None
max_len = None

def clean_text(text):
    text = text.lower()
    text = re.sub(r'http\S+', '', text)
    text = re.sub(r'[^a-zçğıöşü0-9 ]', '', text)
    return text

def load_model_and_tokenizer():
    global model, tokenizer, max_len
    
    try:
        model = tf.keras.models.load_model('../data/spam_model.h5')
        
        with open('../data/tokenizer.pickle', 'rb') as handle:
            tokenizer = pickle.load(handle)
        
        max_len = 51
        return True
    except Exception as e:
        return False

@app.route('/')
def index():
    with open('index.html', 'r', encoding='utf-8') as file:
        return file.read()

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()
        message = data.get('message', '')
        
        if not message:
            return jsonify({'error': 'Mesaj boş olamaz'}), 400
        
        if model is None or tokenizer is None:
            return jsonify({'error': 'Model yüklenmedi'}), 500
        
        cleaned_message = clean_text(message)
        sequence = tokenizer.texts_to_sequences([cleaned_message])
        padded = pad_sequences(sequence, maxlen=max_len, padding='post')
        
        prediction = model.predict(padded)[0][0]
        is_spam = prediction > 0.5
        
        return jsonify({
            'is_spam': bool(is_spam),
            'prediction': float(prediction)
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    load_model_and_tokenizer()
    app.run(debug=True, host='0.0.0.0', port=4100)