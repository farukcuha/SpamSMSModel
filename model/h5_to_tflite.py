import tensorflow as tf
import sys
from pathlib import Path

def convert_h5_to_tflite(h5_model_path, output_tflite_path):
    try:
        model = tf.keras.models.load_model(h5_model_path)
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        converter._experimental_lower_tensor_list_ops = False
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        tflite_model = converter.convert()
        with open(output_tflite_path, 'wb') as f:
            f.write(tflite_model)
        return True
        
    except Exception as e:
        return False

def test_tflite_model(tflite_path):
    try:
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        return True
        
    except Exception as e:
        return False

def main():
    current_dir = Path(__file__).parent
    project_root = current_dir.parent
    
    h5_model_path = project_root / "data" / "spam_model.h5"
    tflite_output_path = project_root / "data" / "spam_model.tflite"
    
    if not h5_model_path.exists():
        sys.exit(1)
    success = convert_h5_to_tflite(str(h5_model_path), str(tflite_output_path))
    if success:
        test_tflite_model(str(tflite_output_path))
    else:
        sys.exit(1)

if __name__ == "__main__":
    main() 