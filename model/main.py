import pandas as pd
import re
import tensorflow as tf
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import pickle


def clean_text(text):
    text = text.lower()
    text = re.sub(r'http\S+', '', text)
    text = re.sub(r'[^a-zçğıöşü0-9 ]', '', text)
    return text

def main():
    df = pd.read_csv('../data/sms_dataset.csv', sep=',')
    df['message_clean'] = df['Message'].apply(clean_text)

    x = df['message_clean'].tolist()
    y = df['spam'].values

    tokenizer = Tokenizer(oov_token="<OOV>")
    tokenizer.fit_on_texts(x)
    sequences = tokenizer.texts_to_sequences(x)

    max_len = max(len(seq) for seq in sequences)
    padded = pad_sequences(sequences, maxlen=max_len, padding='post')

    x_train, x_test, y_train, y_test = train_test_split(padded, y, test_size=0.2, random_state=42)
    vocab_size = len(tokenizer.word_index) + 1

    model = tf.keras.Sequential([
        tf.keras.layers.Embedding(vocab_size, 16, input_length=max_len),
        tf.keras.layers.Bidirectional(tf.keras.layers.LSTM(64)),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])

    model.compile(
        loss='binary_crossentropy',
        optimizer='adam',
        metrics=['accuracy']
    )

    history = model.fit(
        x_train, y_train,
        epochs=10,
        validation_data=(x_test, y_test),
        verbose=1
    )

    model.summary()
    model.save('../data/spam_model.h5')

    y_pred_prob = model.predict(x_test)
    y_pred = (y_pred_prob > 0.5).astype(int).flatten()
    
    display_model_metrics(history, y_test, y_pred)

    tokenizer_json = tokenizer.to_json()
    with open("../data/tokenizer.json", "w") as f:
        f.write(tokenizer_json)

    with open('../data/tokenizer.pickle', 'wb') as handle:
        pickle.dump(tokenizer, handle, protocol=pickle.HIGHEST_PROTOCOL)

def display_model_metrics(history, y_test, y_pred):
    print("\nÖZET")

    final_epoch = len(history.history['loss'])
    final_loss = history.history['loss'][-1]
    final_accuracy = history.history['accuracy'][-1]
    final_val_loss = history.history['val_loss'][-1]
    final_val_accuracy = history.history['val_accuracy'][-1]

    print(f"Total Epochs: {final_epoch}")
    print(f"Final Training Loss: {final_loss:.4f}")
    print(f"Final Training Accuracy: {final_accuracy:.4f}")
    print(f"Final Validation Loss: {final_val_loss:.4f}")
    print(f"Final Validation Accuracy: {final_val_accuracy:.4f}")

    print("\nSınıflandırma Raporu:")
    print(classification_report(y_test, y_pred, target_names=['Normal', 'Spam'], digits=4))

    print("\nKarışıklık Matrisi:")
    cm = confusion_matrix(y_test, y_pred)
    print(f"              Tahmin")
    print(f"Gerçek    Normal  Spam")
    print(f"Normal    {cm[0][0]:6d}  {cm[0][1]:4d}")
    print(f"Spam      {cm[1][0]:6d}  {cm[1][1]:4d}")

    accuracy = accuracy_score(y_test, y_pred)
    print(f"\nGenel Doğruluk (Accuracy): {accuracy:.4f}")

if __name__ == '__main__':
    main()