
package com.motolky.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.motolky.Common;
import com.motolky.Peer;

/**
 * This class is a thread that reads data from a given socket and
 * sends this data to a receive handler. Also it can receive data from a sender
 * and send that data through the socket. It throws exceptions whenever an
 * error occurs on the socket.
 */
/**
 *このクラスは、指定されたソケットからデータを読み込み、スレッドとある
 *受信ハンドラーにこのデータを送信します。また、それは、送信側からデータを受信することができる
 *それとソケットを介してそのデータを送信します。それは、いつでも例外をスローします
 *エラーがソケットで発生します。
 */
public class CommunicationThread extends Thread implements ISendHandler {
    private int LAG_CUT_TIMEOUT = 200;
    private IReceiveHandler mReceiveHandler = null;
    private BluetoothSocket mSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private Peer mPeer = null;
    private boolean mStopped = false;

    /**
     * Constructor
     * @param receiveHandler - the data received from the socket will be sent to this object
     * @param socket - the socket to listen from
     * @param peer - this object needs to be notified when a communication error occurs
     */
/**
 *このクラスは、指定されたソケットからデータを読み込み、スレッドとある
 *受信ハンドラーにこのデータを送信します。また、それは、送信側からデータを受信することができる
 *とソケットを介してそのデータを送信します。それは、いつでも例外をスローします
 *エラーがソケットで発生します。
 *//**
     *コンストラクタ
     *@パラメータreceiveHandler - ソケットから受信したデータは、このオブジェクトに送信されます
     *@パラメータソケット - から聞くためのソケット
     *@パラメータピア - このオブジェクトは、通信エラーが発生したときに通知する必要がある
     */
    public CommunicationThread(IReceiveHandler receiveHandler,
                                BluetoothSocket socket,
                                Peer peer) {
        mReceiveHandler = receiveHandler;
        mSocket = socket;
        mPeer = peer;

        // Create the link to the socket
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error getting the input and the output stream: " + ioe.getMessage());
        }
    }

    /**
     * This method receives a buffer with data and sends it on the socket
     * @param buffer - where the data is
     * @param buffer - how many bytes of data to send from the buffer
     */
/**
     *このメソッドは、データでバッファを受け取り、ソケット上に送信
     *@パラメータバッファ - データがある場合
     *@パラメータバッファ - 何バイトのデータをバッファから送信先
     */
    @Override
    public void sendData(byte[] buffer, int bytes) {
        try {
            mOutputStream.write(buffer, 0, bytes);
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error sending data on socket: " + ioe.getMessage());
            cancel();
            mPeer.communicationErrorOccured();
        }
    }

    /**
     * Stop the thread and close the communication channel
     */
/**
     *スレッドを停止し、通信チャネルを閉じる
     */
    public void cancel() {
        mStopped = true;
        try {
            mSocket.close();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error closing the socket: " + ioe.getMessage());
        }
    }

    /**
     * The thread continuously reads data from socket and sends it to the
     * notifiable object it received in the constructor
     */
/**
     *スレッドが継続的にソケットからデータを読み込み、それを送信します
     それはコンストラクタで受け取っ*届出オブジェクト
     */
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        int times = 0;

        while (!mStopped) {
            try {
                if (times == 0) {
                    try {
                    	// Sometimes lag occurs. Therefore, from time to time
                    	// we just ignore a large chunk of data from input stream
                    	// in order to synchronize the communication.
                        mInputStream.skip(1024000);
                    } catch (Exception e) {}
                    times = LAG_CUT_TIMEOUT;
                }
                times--;

                // Read from socket and send to the handler
                bytes = mInputStream.read(buffer);
                mReceiveHandler.receiveData(buffer, bytes);

            } catch (IOException ioe) {
                Log.e(Common.TAG, "Error receiving from the socket: " + ioe.getMessage());
                mPeer.communicationErrorOccured();
                break;
            }
        }
    }

}
