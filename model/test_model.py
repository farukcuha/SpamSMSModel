import re
import pickle
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.sequence import pad_sequences

def clean_text(text):
    text = text.lower()
    text = re.sub(r'http\S+', '', text)
    text = re.sub(r'[^a-zçğıöşü0-9 ]', '', text)
    return text

def load_spam_model():
    print("loading model and tokenizer...")
    model = load_model('../data/spam_model.h5')
    with open('../data/tokenizer.pickle', 'rb') as f:
        tokenizer = pickle.load(f)
    print("model loaded successfully!")
    return model, tokenizer

def predict_single_message(message, model, tokenizer, max_len=51):
    cleaned = clean_text(message)
    sequence = tokenizer.texts_to_sequences([cleaned])
    padded = pad_sequences(sequence, maxlen=max_len, padding='post')
    
    probability = model.predict(padded, verbose=0)[0][0]
    prediction = 1 if probability > 0.5 else 0
    
    return probability, prediction

def test_message(message, model, tokenizer):
    prob, pred = predict_single_message(message, model, tokenizer)
    
    label = "spam" if pred == 1 else "normal"
    confidence = prob if pred == 1 else (1 - prob)
    
    print(f"\nmessage: '{message}'")
    print(f"result: {label}")
    print(f"confidence score: {confidence:.4f} ({confidence*100:.2f}%)")
    print(f"raw score: {prob:.4f}")
    print("-" * 50)

def interactive_test():
    try:
        model, tokenizer = load_spam_model()
        
        print("\nsms spam detection system")
        print("=" * 50)
        print("enter message and press enter ('q' to exit)")
        print("=" * 50)
        
        while True:
            message = input("\ntest message: ").strip()
            
            if message.lower() == 'q':
                print("\nsee you later!")
                break
            
            if not message:
                print("please enter a message!")
                continue
            
            test_message(message, model, tokenizer)
            
    except FileNotFoundError as e:
        print(f"error: model file not found - {e}")
        print("first run main.py to train the model!")
    except Exception as e:
        print(f"unexpected error: {e}")

def test_sample_messages():
    try:
        model, tokenizer = load_spam_model()
        
        sample_messages = [
            "Merhaba nasılsın? Bugün buluşalım mı?",
            "TEBRİKLER! 1000 TL kazandınız! Hemen tıklayın!",
            "Toplantı yarın saat 14:00'da başlayacak",
            "ACIL! Kredi kartı bilgilerinizi güncelleyin",
            "Akşam yemeğine gelir misin?",
            "Vodafone'dan size özel %70 indirim!",
            "Doktor randevunuz yarın saat 10:00",
            "ŞOK FİYAT! Sadece bugün geçerli kampanya!"
        ]
        
        print("\ntesting sample messages:")
        print("=" * 50)
        
        for message in sample_messages:
            test_message(message, model, tokenizer)
            
    except Exception as e:
        print(f"error: {e}")

def main():
    print("sms spam detection system")
    print("=" * 30)
    print("1. interactive test (enter your own messages)")
    print("2. test sample messages")
    print("=" * 30)
    
    choice = input("choose option (1/2): ").strip()
    
    if choice == "1":
        interactive_test()
    elif choice == "2":
        test_sample_messages()
    else:
        print("invalid choice!")

if __name__ == '__main__':
    main() 