### Informatics Large Practical


The app  is  a  prototype  of  a  multi-player  collaborative  location-based  game  where
players collect and exchange virtual coins which have been scattered at random around
the  University  of  Edinburgh’s  Central  Area.   The  app  should  be  considered  to  be  a
prototype in the sense that it is being created with the intention
of passing it on to a team of developers who will maintain and develop it further. 

In the Coinz game, the locations of the coins are specified on a map.  Coins are collected
by getting near to their location on the map, by which we mean that the player literally
moves to that location with their mobile phone. For the purposes of this game, a player
will be judged to be close enough to a coin to be able to collect it if they are within 25
metres of the coin. A new map is released every day. Each map has fifty coins.

The  coins  which  are  being  collected  belong  to  one  of  four  different  (fictional)  crypto-
currencies:   Penny,  Dollar,  Shilling  and  Quid  (these  are  respectively  denoted  by  the
four-letter  acronyms  PENY,  DOLR,  SHIL,  and  QUID).  The  four  different  currencies
fluctuate in relative value on a daily basis, just as real currencies such as GBP, EUR,
and  USD  do.   The  relative  values  of  the  four  currencies  are  defined  in  terms  of  their
relation  to  a  fifth  currency,  GOLD.  Maps  contain  coins  from  the  currencies  PENY,
DOLR, SHIL and QUID, but no GOLD coins.

Every coin on a map has a value greater than zero and less than ten of its currency.
Because the coins are virtual crypto-coins, not minted physical coins, there is no reason
for them to be in a small number of denominations (1p, 2p, 5p, 10p, 20p, 50p etc), and
their value can be any positive real value less than ten, e.g.  5.72384765123 DOLR or
2.37846111259 QUID.

Maps are made available as Geo-JSON documents, in a JSON format which is specialised
for describing places and geographical features. 

Every map has the same format; it is a FeatureCollection containing a list of Features.
Every Feature has Point geometry with two coordinates,  a longitude and a latitude

There will always be 50 Features in the FeatureCollection of each map, each one being
a cryptocoin. The Features in the map have properties which are metadata which tell us information
about the coin, and also markup which suggests a way to render the feature as a place-
mark on a map.  The markup of “marker-symbol” and “marker-color” can be changed,
or ignored, but the properties of “id”, “value” and “currency” are not to be changed.
Every  coin  has  a  24-digit  hexadecimal  identifier  (“id”)  which  uniquely  identifies  this
coin. The FeatureCollection contains metadata too, most importantly the exchange rates for
each of the four types of currency. 

During gameplay, players collect coins, storing them in a local wallet, and later pay them
into their account in a central bank. However, there is an additional complication, which
brings in an element of collaborative play as well as competitive play.  Namely, on any
given day, you can only pay into your bank account directly a maximum  of  25  coins.
This means that, even if you collect all 50 coins on the map, there will 25 coins which
you will not be able to bank.  These unbankable coins (the ones which would exceed your
25-coins-a-day banking allowance) are referred to as “spare change”.

You can message your spare change to another player,  and they can pay it into their
bank account even though they have not collected those coins themselves.  Coins which
you  receive  from  another  player  can  be  messaged  over  to  your  bank  account,  even  in
the  case  where  you  have  collected  and  paid  in  25  coins  already.   By  exchanging  your
spare change with another player, you can both increase the total that you have in your
respective bank accounts, and thereby make indirect use of coins which were of no use
to you directly.

The object of the game is simply to collect as much money as possible.
