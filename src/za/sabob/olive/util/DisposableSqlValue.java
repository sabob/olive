/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package za.sabob.olive.util;

/**
 * Provides a mechanism for custom a {@link SqlValue} to cleanup it's resources.
 */
public interface DisposableSqlValue extends SqlValue {
        
	/**
	 * Clean up resources held by this value object.
	 */
	void cleanup();
}
