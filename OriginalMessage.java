 public class OriginalMessage {
        MessageType messageType;
        int messageLength;
        byte[] Payload;

        public OriginalMessage(MessageType messageType, byte[] payLoad) {
            this.messageType = messageType;
            this.messageLength = payLoad.length;
            this.Payload = payLoad;


        }

        public MessageType getMessageType() {
             return messageType;
        }
        public void setMessageType(MessageType messageType) {
         this.messageType = messageType;
     }
        public int getMessageLength() {
            return messageLength;
        }
        public void setMessageLength(int messageLength) {
            this.messageLength = messageLength;
        }
        public byte[] getPayload() {
            return Payload;
        }
        public void setPayload(byte[] Payload) {
            this.Payload = Payload;
        }

        public byte[] OriginalMessageInBytes() {
            Integer msgLength = getMessageLength() + 1;
            byte b = ByteArrayHandler.intToByteArray(getMessageType().ordinal())[3];
            byte[] len = ByteArrayHandler.intToByteArray(msgLength);
            byte[] msg = ByteArrayHandler.mergeConstructor(ByteArrayHandler.mergewithByteConstructor(len, b), getPayload());
            return msg;
        }
    }

