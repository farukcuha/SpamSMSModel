import pandas as pd
import matplotlib.pyplot as plt


def create_visualizations(df):
    df['message_length'] = df['Message'].str.len()
    df['word_count'] = df['Message'].str.split().str.len()
    
    fig, axes = plt.subplots(2, 2, figsize=(15, 12))
    
    spam_counts = df['spam'].value_counts()
    axes[0, 0].pie(spam_counts.values, labels=['Ham', 'Spam'], autopct='%1.1f%%', 
                   colors=['lightblue', 'lightcoral'])
    axes[0, 0].set_title('Spam vs Ham Dağılımı')
    
    axes[0, 1].hist(df['message_length'], bins=50, alpha=0.7, color='skyblue')
    axes[0, 1].set_title('Mesaj Uzunluğu Dağılımı')
    axes[0, 1].set_xlabel('Karakter Sayısı')
    axes[0, 1].set_ylabel('Frekans')
    
    ham_lengths = df[df['spam'] == 0]['message_length']
    spam_lengths = df[df['spam'] == 1]['message_length']
    
    axes[1, 0].boxplot([ham_lengths, spam_lengths], labels=['Ham', 'Spam'])
    axes[1, 0].set_title('Ham vs Spam Mesaj Uzunlukları')
    axes[1, 0].set_ylabel('Karakter Sayısı')
    
    axes[1, 1].hist(df['word_count'], bins=30, alpha=0.7, color='lightgreen')
    axes[1, 1].set_title('Kelime Sayısı Dağılımı')
    axes[1, 1].set_xlabel('Kelime Sayısı')
    axes[1, 1].set_ylabel('Frekans')
    
    plt.tight_layout()
    plt.savefig('../data/dataset_analysis.png', dpi=300, bbox_inches='tight')
    plt.show()


def main():
    df = pd.read_csv('../data/sms_dataset.csv', sep=',')
    create_visualizations(df)


if __name__ == '__main__':
    main() 