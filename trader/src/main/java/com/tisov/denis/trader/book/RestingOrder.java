package com.tisov.denis.trader.book;

  public final class RestingOrder {

      private long orderRef;
      private int  locate;
      private long stock;
      private byte side;
      private long price;
      private long shares;

      public void set(long orderRef, int locate, long stock, byte side, long price, long shares) {
          this.orderRef = orderRef;
          this.locate   = locate;
          this.stock    = stock;
          this.side     = side;
          this.price    = price;
          this.shares   = shares;
      }

      public long orderRef() { return orderRef; }
      public int  locate()   { return locate; }
      public long stock()    { return stock; }
      public byte side()     { return side; }
      public long price()    { return price; }
      public long shares()   { return shares; }

      public long reduceShares(long delta) {
          this.shares -= delta;
          return this.shares;
      }
  }
