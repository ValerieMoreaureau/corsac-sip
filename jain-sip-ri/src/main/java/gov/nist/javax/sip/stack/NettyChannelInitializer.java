package gov.nist.javax.sip.stack;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 *  special handler that is purposed to help a user configure a new Channel
 */
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private NettyStreamMessageProcessor nettyStreamMessageProcessor;
    private final SslContext sslCtx;

    public NettyChannelInitializer(
            NettyStreamMessageProcessor nettyStreamMessageProcessor, 
            SslContext sslCtx) {
        this.nettyStreamMessageProcessor = nettyStreamMessageProcessor; 
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {        
        ChannelPipeline pipeline = ch.pipeline();

        // Add SSL handler first to encrypt and decrypt everything.
        // In this example, we use a bogus certificate in the server side
        // and accept any invalid certificates in the client side.
        // You will need something more complicated to identify both
        // and server in the real world.
        if (sslCtx != null) {
            SslHandler sslHandler = sslCtx.newHandler(ch.alloc());            
            if(sslCtx.isClient()) {
                sslHandler.engine().setUseClientMode(true);
            }            
            pipeline.addLast(sslHandler);
            
        }
        // Add support for socket timeout
        if (nettyStreamMessageProcessor.sipStack.nioSocketMaxIdleTime > 0) {
            pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler((int) nettyStreamMessageProcessor.sipStack.nioSocketMaxIdleTime / 1000));
        }
        // Decoders
        pipeline.addLast("NettySIPMessageDecoder",
                        new NettySIPMessageDecoder(nettyStreamMessageProcessor.sipStack));

        // Encoder
        pipeline.addLast("bytesEncoder", new ByteArrayEncoder());

        pipeline.addLast("NettyMessageHandler", new NettyMessageHandler(nettyStreamMessageProcessor));
    }
}
