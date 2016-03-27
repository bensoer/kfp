package lib.db

import tools.AddressPair

/**
 * Created by bensoer on 28/02/16.
 */

public interface IPersistanceAdaptor{

    fun saveAddress(addressPair: AddressPair):Unit;

    fun deleteAddress(addressPair: AddressPair):Unit;

    fun loadAll():Set<AddressPair>;


}