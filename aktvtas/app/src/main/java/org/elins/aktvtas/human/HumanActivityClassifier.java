package org.elins.aktvtas.human;


import android.content.res.AssetManager;
import android.util.Log;

import org.elins.aktvtas.sensor.SensorDataSequence;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class HumanActivityClassifier {
    private static final String TAG = "HumanActivityClassifier";

    private static final String MODEL_FILE =
            "file:///android_asset/human_activity_recognition_graph.pb";

    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output/Softmax:0";

    private static final float THRESHOLD = 0.1f;
    private static final int MAX_RESULT = 3;

    private float[] outputs = new float[HumanActivity.Id.values().length];
    private TensorFlowInferenceInterface inferenceInterface;

    public HumanActivityClassifier(AssetManager assetManager) {
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);
    }

    public List<Recognition> classify(SensorDataSequence sequence) {
        float inputNode[] = sequence.flatten();
        StringBuilder sensorString = new StringBuilder();

        for (float anInputNode : inputNode) {
            sensorString.append(String.format("%f,", anInputNode));
        }
        Log.i(TAG, String.valueOf(inputNode.length));
        Log.i(TAG, sensorString.toString());

        inferenceInterface.feed(INPUT_NAME, inputNode, 600);
        inferenceInterface.run(new String[] {OUTPUT_NAME});
        inferenceInterface.fetch(OUTPUT_NAME, outputs);

        StringBuilder outputString = new StringBuilder();
        for (float output : outputs) {
            outputString.append(String.format("%f, ", output));
        }
        Log.i(TAG, String.valueOf(outputs.length));
        Log.i(TAG, outputString.toString());

        return findBestClassification(outputs);
    }

    public List<Recognition> findBestClassification(float[] outputNode) {
        PriorityQueue<Recognition> queue =
            new PriorityQueue<>(
                3,
                new Comparator<Recognition>() {
                    @Override
                    public int compare(Recognition lhs, Recognition rhs) {
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                 }
            );

        for (int i = 0; i < outputNode.length; i++) {
            if (outputNode[i] > THRESHOLD) {
                queue.add(new Recognition(i, outputNode[i]));
            }
        }

        final List<Recognition> recognitions = new ArrayList<>();
        int recognitionSize = Math.min(queue.size(), MAX_RESULT);

        for (int i = 0; i < recognitionSize; ++i) {
            recognitions.add(queue.poll());
        }

        return recognitions;
    }
}
